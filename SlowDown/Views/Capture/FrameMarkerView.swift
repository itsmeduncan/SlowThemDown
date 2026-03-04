import SwiftUI

struct FrameMarkerView: View {
    let title: String
    let image: UIImage
    let imageSize: CGSize
    let markers: [CGPoint]
    var markerColor: Color = .accentColor
    let onTap: (CGPoint, CGSize) -> Void
    let onConfirm: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text(title)
                .font(.headline)

            Text("Tap the same point on the vehicle (e.g. front bumper)")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            ImageMarkerOverlay(
                image: image,
                markers: markers,
                imageSize: imageSize,
                markerColor: markerColor
            ) { point, size in
                onTap(point, size)
            }
            .frame(height: 300)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            if markers.count == 1 {
                Button {
                    onConfirm()
                } label: {
                    Label("Confirm Marker", systemImage: "checkmark.circle")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundStyle(.black)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            } else {
                Text("Tap on the vehicle to place a marker")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
    }
}
