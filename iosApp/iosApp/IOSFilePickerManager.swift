import UIKit
import UniformTypeIdentifiers
import Foundation
import ComposeApp

/**
 * iOS File Picker Manager implementation that conforms to the KMP FilePickerManager interface
 */
class IOSFilePickerManager: NSObject, FilePickerManager {

    private var completionHandler: ((FileData?) -> Void)?
    private var documentPicker: UIDocumentPickerViewController?

    func pickFile(mimeTypes: KotlinArray<NSString>, onResult: @escaping (FileData?) -> Void) {
        self.completionHandler = onResult

        // Convert MIME types to UTType identifiers
        var utTypes: [UTType] = []

        for i in 0..<mimeTypes.size {
            let mimeType = mimeTypes.get(index: i) as! String

            switch mimeType {
            case let type where type.hasPrefix("image/"):
                switch type {
                case "image/*":
                    if let utType = UTType("public.image") {
                        utTypes.append(utType)
                    }
                case "image/jpeg":
                    if let utType = UTType("public.jpeg") {
                        utTypes.append(utType)
                    }
                case "image/png":
                    if let utType = UTType("public.png") {
                        utTypes.append(utType)
                    }
                case "image/heic":
                    if let utType = UTType("public.heic") {
                        utTypes.append(utType)
                    }
                case "image/heif":
                    if let utType = UTType("public.heif") {
                        utTypes.append(utType)
                    }
                default:
                    // Fallback to generic image type
                    if let utType = UTType("public.image") {
                        utTypes.append(utType)
                    }
                }
            case let type where type.hasPrefix("application/pdf"):
                if let utType = UTType("com.adobe.pdf") {
                    utTypes.append(utType)
                }
            case "*/*":
                if let utType = UTType("public.data") {
                    utTypes.append(utType)
                }
            default:
                // Try to create UTType from MIME type
                if let utType = UTType(mimeType: mimeType) {
                    utTypes.append(utType)
                }
            }
        }

        // Fallback to general data type if no specific types were added
        if utTypes.isEmpty {
            if let utType = UTType("public.data") {
                utTypes.append(utType)
            }
        }

        presentDocumentPicker(utTypes: utTypes)
    }

    private func presentDocumentPicker(utTypes: [UTType]) {
        guard let rootViewController = getRootViewController() else {
            print("No root view controller available")
            completionHandler?(nil)
            return
        }

        let picker = UIDocumentPickerViewController(forOpeningContentTypes: utTypes, asCopy: true)
        picker.delegate = self
        picker.allowsMultipleSelection = false
        picker.modalPresentationStyle = .formSheet

        self.documentPicker = picker
        rootViewController.present(picker, animated: true)
    }

    private func getRootViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else {
            return nil
        }
        return window.rootViewController
    }

    private func convertToFileData(from url: URL) -> FileData? {
        guard url.startAccessingSecurityScopedResource() else {
            print("Failed to access security-scoped resource")
            return nil
        }

        defer {
            url.stopAccessingSecurityScopedResource()
        }

        do {
            let data = try Data(contentsOf: url)
            let filename = url.lastPathComponent

            // Convert Data to KotlinByteArray for FileManager to save
            let byteArray = data.withUnsafeBytes { (bytes: UnsafeRawBufferPointer) in
                KotlinByteArray(size: Int32(data.count)) { index in
                    let i = Int(index.int32Value)
                    let byte = bytes.load(fromByteOffset: i, as: UInt8.self)
                    return KotlinByte(value: Int8(bitPattern: byte))
                }
            }

            // Determine MIME type
            let mimeType = determineMimeType(from: url)

            // Get Documents/BudgetHunter/Invoices directory for FileManager
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let budgetHunterDirectory = documentsDirectory.appendingPathComponent("BudgetHunter", isDirectory: true)
            let invoicesDirectory = budgetHunterDirectory.appendingPathComponent("Invoices", isDirectory: true)
            let persistentDirectory = invoicesDirectory.path

            // Create timestamp filename to match Android format (timestamp + extension)
            let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
            let fileExtension = "." + url.pathExtension
            let persistentFileName = "\(timestamp)\(fileExtension)"

            print("FilePickerInterface: Will let FileManager save to: \(persistentDirectory)/\(persistentFileName)")

            return FileData(
                data: byteArray,
                filename: persistentFileName,
                mimeType: mimeType,
                directory: persistentDirectory
            )

        } catch {
            print("Error reading file: \(error.localizedDescription)")
            return nil
        }
    }

    private func determineMimeType(from url: URL) -> String? {
        let pathExtension = url.pathExtension.lowercased()

        switch pathExtension {
        case "jpg", "jpeg":
            return "image/jpeg"
        case "png":
            return "image/png"
        case "heic":
            return "image/heic"
        case "heif":
            return "image/heif"
        case "pdf":
            return "application/pdf"
        case "txt":
            return "text/plain"
        case "json":
            return "application/json"
        default:
            // Try to get MIME type from UTType
            if let utType = UTType(filenameExtension: pathExtension) {
                return utType.preferredMIMEType
            }
            return nil
        }
    }
}

// MARK: - UIDocumentPickerDelegate
extension IOSFilePickerManager: UIDocumentPickerDelegate {

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        controller.dismiss(animated: true)

        guard let selectedUrl = urls.first else {
            completionHandler?(nil)
            return
        }

        print("File picker successful - selected: \(selectedUrl.lastPathComponent)")
        let fileData = convertToFileData(from: selectedUrl)
        completionHandler?(fileData)

        // Clean up
        completionHandler = nil
        documentPicker = nil
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        controller.dismiss(animated: true)

        print("File picker cancelled")
        completionHandler?(nil)

        // Clean up
        completionHandler = nil
        documentPicker = nil
    }
}