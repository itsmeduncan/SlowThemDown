import SwiftUI

struct ContentView: View {
    @AppStorage("onboarding_completed") private var onboardingCompleted = false

    var body: some View {
        if onboardingCompleted {
            TabView {
                CaptureView()
                    .tabItem {
                        Label("Capture", systemImage: "video.fill")
                    }

                CalibrateView()
                    .tabItem {
                        Label("Calibrate", systemImage: "ruler")
                    }

                LogView()
                    .tabItem {
                        Label("Log", systemImage: "list.bullet")
                    }

                ReportView()
                    .tabItem {
                        Label("Reports", systemImage: "chart.bar.fill")
                    }
            }
        } else {
            OnboardingView {
                onboardingCompleted = true
            }
        }
    }
}
