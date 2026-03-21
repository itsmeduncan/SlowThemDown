import Testing
@testable import SlowThemDown

@Suite("PIIBlurService – isLikelyPlate heuristic")
struct PIIBlurServiceTests {

    // MARK: - Valid plates

    @Test func validPlate_standardFormat() {
        #expect(PIIBlurService.isLikelyPlate(text: "ABC 1234", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func validPlate_noSpaces() {
        #expect(PIIBlurService.isLikelyPlate(text: "7XYZ123", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func validPlate_withHyphen() {
        #expect(PIIBlurService.isLikelyPlate(text: "AB-1234", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func validPlate_fiveChars() {
        #expect(PIIBlurService.isLikelyPlate(text: "AB123", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func validPlate_eightChars() {
        #expect(PIIBlurService.isLikelyPlate(text: "ABCD1234", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    // MARK: - Non-plates

    @Test func nonPlate_allLetters() {
        #expect(!PIIBlurService.isLikelyPlate(text: "STOP", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func nonPlate_allDigits() {
        #expect(!PIIBlurService.isLikelyPlate(text: "1425", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func nonPlate_tooShort() {
        #expect(!PIIBlurService.isLikelyPlate(text: "A1", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func nonPlate_tooLong() {
        #expect(!PIIBlurService.isLikelyPlate(text: "ABCDE12345", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func nonPlate_streetName() {
        #expect(!PIIBlurService.isLikelyPlate(text: "MAIN ST", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    @Test func nonPlate_allLettersFiveChars() {
        #expect(!PIIBlurService.isLikelyPlate(text: "ABCDE", bboxWidth: 200, bboxHeight: 100, imageWidth: 1920))
    }

    // MARK: - Aspect ratio edge cases

    @Test func nonPlate_aspectRatioTooTall() {
        // 100w x 200h = 0.5 aspect ratio
        #expect(!PIIBlurService.isLikelyPlate(text: "ABC1234", bboxWidth: 100, bboxHeight: 200, imageWidth: 1920))
    }

    @Test func nonPlate_aspectRatioTooWide() {
        // 400w x 100h = 4.0 aspect ratio
        #expect(!PIIBlurService.isLikelyPlate(text: "ABC1234", bboxWidth: 400, bboxHeight: 100, imageWidth: 1920))
    }

    // MARK: - Size edge cases

    @Test func nonPlate_tooSmall() {
        // 10w on 1920 image = 0.5% < 1% threshold
        #expect(!PIIBlurService.isLikelyPlate(text: "ABC1234", bboxWidth: 10, bboxHeight: 5, imageWidth: 1920))
    }

    @Test func nonPlate_zeroHeight() {
        #expect(!PIIBlurService.isLikelyPlate(text: "ABC1234", bboxWidth: 200, bboxHeight: 0, imageWidth: 1920))
    }
}
