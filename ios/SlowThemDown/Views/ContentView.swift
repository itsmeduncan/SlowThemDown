import SwiftUI

enum AppTab: String {
    case capture, calibrate, log, reports
}

struct ContentView: View {
    @AppStorage("onboarding_completed") private var onboardingCompleted = false
    @State private var selectedTab: AppTab = .capture
    @State private var justFinishedOnboarding = false

    var body: some View {
        if onboardingCompleted {
            TabView(selection: $selectedTab) {
                CaptureView()
                    .tabItem {
                        Label("Capture", systemImage: "video.fill")
                    }
                    .tag(AppTab.capture)

                CalibrateView()
                    .tabItem {
                        Label("Calibrate", systemImage: "ruler")
                    }
                    .tag(AppTab.calibrate)

                LogView()
                    .tabItem {
                        Label("Log", systemImage: "list.bullet")
                    }
                    .tag(AppTab.log)

                ReportView()
                    .tabItem {
                        Label("Reports", systemImage: "chart.bar.fill")
                    }
                    .tag(AppTab.reports)
            }
            .onAppear {
                if justFinishedOnboarding {
                    selectedTab = .calibrate
                    justFinishedOnboarding = false
                }
            }
        } else {
            OnboardingView {
                justFinishedOnboarding = true
                onboardingCompleted = true
            }
        }
    }
}
