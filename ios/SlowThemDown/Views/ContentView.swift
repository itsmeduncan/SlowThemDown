import SwiftUI

enum AppTab: String {
    case capture, log, reports, calibrate
}

struct ContentView: View {
    @AppStorage("onboarding_completed") private var onboardingCompleted = false
    @State private var selectedTab: AppTab = .capture
    @State private var justFinishedOnboarding = false
    @State private var calibrationVM = CalibrationViewModel()

    var body: some View {
        if onboardingCompleted {
            TabView(selection: $selectedTab) {
                CaptureView()
                    .tabItem {
                        Label("Capture", systemImage: "video.fill")
                    }
                    .tag(AppTab.capture)

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

                CalibrateView()
                    .tabItem {
                        Label("Calibrate", systemImage: calibrationVM.isCalibrated
                            ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                    }
                    .tag(AppTab.calibrate)
                    .badge(calibrationVM.isCalibrated ? nil : "!")
            }
            .onAppear {
                if justFinishedOnboarding {
                    selectedTab = .calibrate
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
