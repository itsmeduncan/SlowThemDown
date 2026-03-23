import SwiftUI

struct ImageMarkerOverlay: View {
    let image: UIImage
    let markers: [CGPoint]
    let imageSize: CGSize
    var markerColor: Color = .accentColor
    var onTap: ((CGPoint, CGSize) -> Void)?

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .accessibilityLabel("Reference image with \(markers.count) marker\(markers.count == 1 ? "" : "s") placed")

                // Draw markers
                ForEach(Array(markers.enumerated()), id: \.offset) { index, imagePoint in
                    let viewPoint = CoordinateMapper.imageToView(
                        imagePoint: imagePoint,
                        viewSize: geo.size,
                        imageSize: imageSize
                    )
                    ZStack {
                        Circle()
                            .fill(markerColor)
                            .frame(width: 20, height: 20)
                        Circle()
                            .stroke(.white, lineWidth: 2)
                            .frame(width: 20, height: 20)
                        Text("\(index + 1)")
                            .font(.caption2.bold())
                            .foregroundStyle(.white)
                    }
                    .accessibilityLabel("Marker \(index + 1)")
                    .position(viewPoint)
                }

                // Draw line between markers
                if markers.count == 2 {
                    let p1 = CoordinateMapper.imageToView(
                        imagePoint: markers[0],
                        viewSize: geo.size,
                        imageSize: imageSize
                    )
                    let p2 = CoordinateMapper.imageToView(
                        imagePoint: markers[1],
                        viewSize: geo.size,
                        imageSize: imageSize
                    )
                    Path { path in
                        path.move(to: p1)
                        path.addLine(to: p2)
                    }
                    .stroke(markerColor, style: StrokeStyle(lineWidth: 2, dash: [6, 3]))
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { location in
                guard CoordinateMapper.isWithinImageBounds(
                    viewPoint: location, viewSize: geo.size, imageSize: imageSize
                ) else { return }
                onTap?(location, geo.size)
            }
        }
    }
}
