import SwiftUI

struct FrameSelectorView: View {
    @Binding var frame1Time: Double
    @Binding var frame2Time: Double
    let duration: Double
    let onConfirm: () -> Void

    private var timeDelta: Double {
        abs(frame2Time - frame1Time)
    }

    var body: some View {
        VStack(spacing: 20) {
            Text("Select Two Frames")
                .font(.headline)

            Text("Choose the start and end points when the vehicle is visible")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            VStack(spacing: 12) {
                VStack(alignment: .leading) {
                    Text("Frame 1: \(frame1Time, specifier: "%.3f")s")
                        .font(.caption.monospacedDigit())
                    Slider(value: $frame1Time, in: 0...duration, step: 0.001)
                        .tint(.blue)
                }

                VStack(alignment: .leading) {
                    Text("Frame 2: \(frame2Time, specifier: "%.3f")s")
                        .font(.caption.monospacedDigit())
                    Slider(value: $frame2Time, in: 0...duration, step: 0.001)
                        .tint(.orange)
                }
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
                Label("Extract Frames", systemImage: "photo.on.rectangle.angled")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(timeDelta >= 0.01 ? Color.accentColor : Color.gray)
                    .foregroundStyle(.black)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(timeDelta < 0.01)
        }
        .padding()
    }
}
