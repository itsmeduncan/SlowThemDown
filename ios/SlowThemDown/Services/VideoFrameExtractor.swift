import AVFoundation
import UIKit

actor VideoFrameExtractor {
    private let asset: AVAsset

    init(url: URL) {
        self.asset = AVAsset(url: url)
    }

    var duration: CMTime {
        get async throws {
            try await asset.load(.duration)
        }
    }

    var naturalSize: CGSize {
        get async throws {
            guard let track = try await asset.loadTracks(withMediaType: .video).first else {
                return .zero
            }
            return try await track.load(.naturalSize)
        }
    }

    /// Extract a frame at `time`.
    ///
    /// Pass `maximumSize` to get a downscaled image — used for scrub previews, where
    /// decoding at full resolution on every slider tick is wasteful. Seeking stays
    /// frame-accurate either way, so a preview always shows the frame that a
    /// full-resolution extraction at the same timestamp would produce.
    func extractFrame(at time: CMTime, maximumSize: CGSize? = nil) async throws -> UIImage {
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.requestedTimeToleranceBefore = .zero
        generator.requestedTimeToleranceAfter = .zero
        if let maximumSize {
            generator.maximumSize = maximumSize
        }

        let (cgImage, _) = try await generator.image(at: time)
        return UIImage(cgImage: cgImage)
    }

    func extractFrames(at times: [CMTime]) async throws -> [CMTime: UIImage] {
        var result: [CMTime: UIImage] = [:]
        for time in times {
            let image = try await extractFrame(at: time)
            result[time] = image
        }
        return result
    }
}
