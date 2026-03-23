import SwiftUI

enum AppTab: String {
    case capture, reports, settings
}

struct ContentView: View {
    @AppStorage("onboarding_completed") private var onboardingCompleted = false
    @State private var selectedTab: AppTab = .capture
    @State private var justFinishedOnboarding = false
    @State private var calibrationVM = CalibrationViewModel()

    var body: some View {
        if onboardingCompleted {
            TabView(selection: $selectedTab) {
                HomeView()
                    .tabItem {
                        Label("Capture", systemImage: "video.fill")
                    }
                    .tag(AppTab.capture)

                ReportView()
                    .tabItem {
                        Label("Reports", systemImage: "chart.bar.fill")
                    }
                    .tag(AppTab.reports)

                CalibrateView()
                    .tabItem {
                        Label("Settings", systemImage: calibrationVM.isCalibrated
                            ? "gearshape.fill" : "exclamationmark.triangle.fill")
                    }
                    .tag(AppTab.settings)
                    .badge(calibrationVM.isCalibrated ? nil : "!")
            }
            .onAppear {
                if justFinishedOnboarding {
                    selectedTab = .settings
                    justFinishedOnboarding = false
                }
            }
            .onChange(of: selectedTab) {
                calibrationVM.reload()
            }
        } else {
            OnboardingView {
                justFinishedOnboarding = true
                onboardingCompleted = true
            }
        }
    }
}
