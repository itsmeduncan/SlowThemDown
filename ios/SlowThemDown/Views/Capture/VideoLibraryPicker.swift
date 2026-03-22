import PhotosUI
import SwiftUI

struct VideoLibraryPicker: UIViewControllerRepresentable {
    let onVideoSelected: (URL) -> Void
    let onCancel: () -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration()
        config.filter = .videos
        config.selectionLimit = 1
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onVideoSelected: onVideoSelected, onCancel: onCancel)
    }

    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let onVideoSelected: (URL) -> Void
        let onCancel: () -> Void

        init(onVideoSelected: @escaping (URL) -> Void, onCancel: @escaping () -> Void) {
            self.onVideoSelected = onVideoSelected
            self.onCancel = onCancel
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard let provider = results.first?.itemProvider,
                  provider.hasItemConformingToTypeIdentifier(UTType.movie.identifier) else {
                onCancel()
                return
            }

            provider.loadFileRepresentation(forTypeIdentifier: UTType.movie.identifier) { [weak self] url, _ in
                guard let url else {
                    DispatchQueue.main.async { self?.onCancel() }
                    return
                }

                // PHPicker provides a temporary file that gets deleted after this callback.
                // Copy it to our own temp location so it survives.
                let dest = FileManager.default.temporaryDirectory
                    .appendingPathComponent(UUID().uuidString)
                    .appendingPathExtension(url.pathExtension)
                do {
                    try FileManager.default.copyItem(at: url, to: dest)
                    DispatchQueue.main.async { self?.onVideoSelected(dest) }
                } catch {
                    DispatchQueue.main.async { self?.onCancel() }
                }
            }
        }
    }
}
