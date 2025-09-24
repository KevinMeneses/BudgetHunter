import UIKit
import AVFoundation
import Foundation
import ComposeApp

/**
 * iOS Camera Manager implementation that conforms to the KMP CameraManager interface
 */
class IOSCameraManager: NSObject, CameraManager {

    private var completionHandler: ((FileData?) -> Void)?
    private var imagePickerController: UIImagePickerController?

    func takePhoto(onResult: @escaping (FileData?) -> Void) {
        self.completionHandler = onResult

        // Check if camera is available
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            print("Camera not available on this device")
            onResult(nil)
            return
        }

        // Check camera permissions
        checkCameraPermission { [weak self] granted in
            DispatchQueue.main.async {
                if granted {
                    self?.presentCamera()
                } else {
                    print("Camera permission denied")
                    onResult(nil)
                }
            }
        }
    }

    private func checkCameraPermission(completion: @escaping (Bool) -> Void) {
        let status = AVCaptureDevice.authorizationStatus(for: .video)

        switch status {
        case .authorized:
            completion(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                completion(granted)
            }
        case .denied, .restricted:
            completion(false)
        @unknown default:
            completion(false)
        }
    }

    private func presentCamera() {
        guard let rootViewController = getRootViewController() else {
            print("No root view controller available")
            completionHandler?(nil)
            return
        }

        let imagePicker = UIImagePickerController()
        imagePicker.sourceType = .camera
        imagePicker.mediaTypes = ["public.image"]
        imagePicker.allowsEditing = false
        imagePicker.delegate = self

        self.imagePickerController = imagePicker
        rootViewController.present(imagePicker, animated: true)
    }

    private func getRootViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else {
            return nil
        }
        return window.rootViewController
    }

    private func convertToFileData(imageData: Data) -> FileData {
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let filename = "camera_capture_\(timestamp).jpg"

        // Get documents directory
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?.path ?? ""

        // Convert Data to KotlinByteArray
        let byteArray = imageData.withUnsafeBytes { (bytes: UnsafeRawBufferPointer) in
            KotlinByteArray(size: Int32(imageData.count)) { index in
                let i = Int(index.int32Value)
                let byte = bytes.load(fromByteOffset: i, as: UInt8.self)
                return KotlinByte(value: Int8(bitPattern: byte))
            }
        }

        return FileData(data: byteArray, filename: filename, mimeType: "image/jpeg", directory: documentsPath)
    }
}

// MARK: - UIImagePickerControllerDelegate
extension IOSCameraManager: UIImagePickerControllerDelegate, UINavigationControllerDelegate {

    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        picker.dismiss(animated: true)

        guard let image = info[.originalImage] as? UIImage,
              let imageData = image.jpegData(compressionQuality: 0.8) else {
            completionHandler?(nil)
            return
        }

        print("Camera capture successful - image size: \(imageData.count) bytes")
        let fileData = convertToFileData(imageData: imageData)
        completionHandler?(fileData)

        // Clean up
        completionHandler = nil
        imagePickerController = nil
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)

        print("Camera capture cancelled")
        completionHandler?(nil)

        // Clean up
        completionHandler = nil
        imagePickerController = nil
    }
}