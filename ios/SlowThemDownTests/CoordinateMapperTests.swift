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

    // MARK: - isWithinImageBounds

    @Test func isWithinImageBounds_centerOfImage_returnsTrue() {
        let viewSize = CGSize(width: 390, height: 844)
        let imageSize = CGSize(width: 1920, height: 1080)
        let center = CGPoint(x: 195, y: 422)

        #expect(CoordinateMapper.isWithinImageBounds(
            viewPoint: center, viewSize: viewSize, imageSize: imageSize
        ))
    }

    @Test func isWithinImageBounds_outsideLetterbox_returnsFalse() {
        // Wide image in tall view → letterboxed top/bottom
        let viewSize = CGSize(width: 390, height: 844)
        let imageSize = CGSize(width: 1920, height: 1080)

        // Tap in the top letterbox area (y near 0)
        #expect(!CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 195, y: 5), viewSize: viewSize, imageSize: imageSize
        ))

        // Tap in the bottom letterbox area (y near max)
        #expect(!CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 195, y: 840), viewSize: viewSize, imageSize: imageSize
        ))
    }

    @Test func isWithinImageBounds_outsidePillarbox_returnsFalse() {
        // Tall image in wide view → pillarboxed left/right
        let viewSize = CGSize(width: 844, height: 390)
        let imageSize = CGSize(width: 1080, height: 1920)

        // Tap in left pillarbox
        #expect(!CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 5, y: 195), viewSize: viewSize, imageSize: imageSize
        ))

        // Tap in right pillarbox
        #expect(!CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 840, y: 195), viewSize: viewSize, imageSize: imageSize
        ))
    }

    @Test func isWithinImageBounds_exactFit_allPointsValid() {
        // Image aspect ratio matches view exactly → no letterbox/pillarbox
        let viewSize = CGSize(width: 400, height: 300)
        let imageSize = CGSize(width: 800, height: 600)

        // Origin corner
        #expect(CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 0, y: 0), viewSize: viewSize, imageSize: imageSize
        ))
        // Just inside the far corner (CGRect.contains excludes the max edge)
        #expect(CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 399, y: 299), viewSize: viewSize, imageSize: imageSize
        ))
    }

    @Test func isWithinImageBounds_zeroSizes_returnsFalse() {
        #expect(!CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 10, y: 10), viewSize: .zero, imageSize: CGSize(width: 100, height: 100)
        ))
        #expect(!CoordinateMapper.isWithinImageBounds(
            viewPoint: CGPoint(x: 10, y: 10), viewSize: CGSize(width: 100, height: 100), imageSize: .zero
        ))
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
