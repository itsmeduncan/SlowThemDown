import SwiftUI

private struct OnboardingPage {
    let icon: String
    let title: String
    let description: String
}

private let pages: [OnboardingPage] = [
    OnboardingPage(
        icon: "speedometer",
        title: "Measure Traffic Speeds",
        description: "Slow Them Down estimates vehicle speeds on your street using video from your phone. No radar gun needed — just point, record, and mark."
    ),
    OnboardingPage(
        icon: "ruler",
        title: "Calibrate First",
        description: "Before capturing speeds, calibrate by marking a known distance in a photo — like a lane width or parking space. This tells the app how to convert pixels to real-world feet."
    ),
    OnboardingPage(
        icon: "video.fill",
        title: "Record & Mark",
        description: "Record a short video of passing traffic. Pick two frames, then tap the same point on the vehicle in each frame. Slow Them Down calculates the speed from the displacement."
    ),
    OnboardingPage(
        icon: "car.fill",
        title: "Or Use Vehicle Reference",
        description: "No calibration needed — select a vehicle make from the built-in table during capture. Mark the front and rear bumper, and Slow Them Down uses the known vehicle length."
    ),
    OnboardingPage(
        icon: "chart.bar.fill",
        title: "Build Your Case",
        description: "Track observations over time, calculate V85 speeds (the metric traffic engineers use), and export reports to share with local officials."
    ),
]

struct OnboardingView: View {
    var onComplete: () -> Void

    @State private var currentPage = 0

    var body: some View {
        VStack(spacing: 0) {
            // Skip button
            HStack {
                Spacer()
                if currentPage < pages.count - 1 {
                    Button("Skip") {
                        onComplete()
                    }
                    .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            .frame(height: 44)

            // Page content
            TabView(selection: $currentPage) {
                ForEach(pages.indices, id: \.self) { index in
                    PageContent(page: pages[index])
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))

            // Navigation button
            Button {
                if currentPage < pages.count - 1 {
                    withAnimation {
                        currentPage += 1
                    }
                } else {
                    onComplete()
                }
            } label: {
                Text(currentPage < pages.count - 1 ? "Next" : "Get Started")
                    .font(.title3.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
    }
}

private struct PageContent: View {
    let page: OnboardingPage

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: page.icon)
                .font(.system(size: 72))
                .foregroundStyle(.tint)

            Text(page.title)
                .font(.title.weight(.bold))
                .multilineTextAlignment(.center)

            Text(page.description)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Spacer()
        }
        .padding(.horizontal, 24)
    }
}
