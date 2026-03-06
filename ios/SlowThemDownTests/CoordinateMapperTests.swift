import Testing
import CoreGraphics
@testable import SlowThemDown

@Suite("CoordinateMapper")
struct CoordinateMapperTests {

    // MARK: - viewToImage / imageToView roundtrip

    @Test func viewToImage_imageToView_roundtrip() {
        let viewSize = CGSize(width: 390, height: 844)
        let imageSize = CGSize(width: 1920, height: 1080)
        let original = CGPoint(x: 195, y: 422)

        let imagePoint = CoordinateMapper.viewToImage(
            viewPoint: original, viewSize: viewSize, imageSize: imageSize
        )
        let roundtripped = CoordinateMapper.imageToView(
            imagePoint: imagePoint, viewSize: viewSize, imageSize: imageSize
        )

        #expect(abs(roundtripped.x - original.x) < 0.001)
        #expect(abs(roundtripped.y - original.y) < 0.001)
    }

    @Test func viewToImage_zeroViewSize_returnsZero() {
        let result = CoordinateMapper.viewToImage(
            viewPoint: CGPoint(x: 10, y: 10),
            viewSize: .zero,
            imageSize: CGSize(width: 1920, height: 1080)
        )
        #expect(result == .zero)
    }

    @Test func imageToView_zeroImageSize_returnsZero() {
        let result = CoordinateMapper.imageToView(
            imagePoint: CGPoint(x: 10, y: 10),
            viewSize: CGSize(width: 390, height: 844),
            imageSize: .zero
        )
        #expect(result == .zero)
    }

    // MARK: - pixelDistance

    @Test func pixelDistance_knownValues() {
        let d = CoordinateMapper.pixelDistance(
            from: CGPoint(x: 0, y: 0),
            to: CGPoint(x: 3, y: 4)
        )
        #expect(abs(d - 5.0) < 0.001)
    }

    @Test func pixelDistance_samePoint_isZero() {
        let p = CGPoint(x: 100, y: 200)
        #expect(CoordinateMapper.pixelDistance(from: p, to: p) == 0)
    }
}
