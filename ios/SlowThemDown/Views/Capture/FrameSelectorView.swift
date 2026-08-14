import SwiftUI

struct FrameSelectorView: View {
    @Binding var frame1Time: Double
    @Binding var frame2Time: Double
    let duration: Double
    var preview1: UIImage?
    var preview2: UIImage?
    var isLoadingPreview1: Bool = false
    var isLoadingPreview2: Bool = false
    var isExtracting: Bool = false
    let onConfirm: () -> Void

    private var timeDelta: Double {
        abs(frame2Time - frame1Time)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text("Select Two Frames")
                    .font(.headline)

                Text("Choose the start and end points when the vehicle is visible")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                VStack(spacing: 20) {
                    frameScrubber(
                        label: "Frame 1",
                        time: $frame1Time,
                        preview: preview1,
                        isLoading: isLoadingPreview1,
                        tint: .blue
                    )

                    frameScrubber(
                        label: "Frame 2",
                        time: $frame2Time,
                        preview: preview2,
                        isLoading: isLoadingPreview2,
                        tint: .orange
                    )
                }

                HStack {
                    Image(systemName: "timer")
                    Text("Time delta: \(timeDelta, specifier: "%.3f")s")
                        .font(.system(.body, design: .monospaced, weight: .semibold))
                }
                .padding()
                .background(Color(.systemGray6).opacity(0.3))
                .clipShape(RoundedRectangle(cornerRadius: 8))

                if timeDelta < 0.01 {
                    Text("Frames are too close together. Select frames further apart.")
                        .font(.caption)
                        .foregroundStyle(.red)
                }

                Button {
                    onConfirm()
                } label: {
                    HStack(spacing: 8) {
                        if isExtracting {
                            ProgressView()
                                .tint(.black)
                            Text("Extracting Frames…")
                        } else {
                            Label("Extract Frames", systemImage: "photo.on.rectangle.angled")
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(timeDelta >= 0.01 && !isExtracting ? Color.accentColor : Color.gray)
                    .foregroundStyle(.black)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(timeDelta < 0.01 || isExtracting)
            }
            .padding()
        }
    }

    private func frameScrubber(
        label: LocalizedStringKey,
        time: Binding<Double>,
        preview: UIImage?,
        isLoading: Bool,
        tint: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            FramePreview(image: preview, isLoading: isLoading)

            HStack {
                Text(label)
                    .font(.caption.weight(.semibold))
                Spacer()
                Text("\(time.wrappedValue, specifier: "%.3f")s")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)
            }

            Slider(value: time, in: 0...max(duration, 0.001), step: 0.001)
                .tint(tint)
        }
    }
}

/// Shows the frame at the current scrub position. Keeps the last good image on
/// screen while the next one decodes, so dragging doesn't flash empty.
private struct FramePreview: View {
    let image: UIImage?
    let isLoading: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemGray6).opacity(0.3))

            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
            } else if !isLoading {
                Image(systemName: "photo")
                    .font(.title2)
                    .foregroundStyle(.secondary)
            }

            if isLoading {
                ProgressView()
                    .controlSize(.small)
            }
        }
        .frame(height: 160)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
