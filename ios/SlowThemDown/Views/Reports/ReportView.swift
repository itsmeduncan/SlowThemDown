import CoreLocation
import MessageUI
import SwiftData
import SwiftUI

struct ReportView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \SpeedEntry.timestamp, order: .reverse) private var entries: [SpeedEntry]
    @State private var vm = ReportViewModel()
    @State private var showShareSheet = false
    @State private var shareItems: [Any] = []
    @State private var isExporting = false
    @State private var showingDemoData = SeedData.isSeeded
    @State private var agencyPickerItem: AgencyPickerItem?
    @State private var showMailComposer = false
    @State private var selectedAgency: Agency?
    @State private var agencyPdfURL: URL?
    @State private var agencyCsvURL: URL?
    @State private var locationManager = LocationManager()

    @AppStorage("measurementSystem") private var measurementSystemRaw: String = MeasurementSystem.deviceDefault.rawValue

    private var measurementSystem: MeasurementSystem {
        MeasurementSystem(rawValue: measurementSystemRaw) ?? .imperial
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                if showingDemoData {
                    DemoBanner {
                        SeedData.clearDemoData(context: modelContext)
                        showingDemoData = false
                    }
                    .padding(.horizontal)
                }

                if entries.isEmpty {
                    ContentUnavailableView(
                        "No Data",
                        systemImage: "chart.bar",
                        description: Text("Capture some speed measurements to see reports.")
                    )
                } else {
                    VStack(spacing: 20) {
                        streetFilterSection
                        v85Section
                        metricsGrid
                        histogramChart
                        hourlyChart
                        scatterChart
                        if vm.selectedStreet == nil && vm.streetGroups.count > 1 {
                            streetBreakdownSection
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Reports")
            .toolbar {
                if !entries.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        Menu {
                            Button {
                                exportCSV()
                            } label: {
                                Label("Export CSV", systemImage: "tablecells")
                            }
                            Button {
                                exportPDF()
                            } label: {
                                Label("Export PDF", systemImage: "doc.richtext")
                            }
                            Divider()
                            Button {
                                loadMatchedAgencies()
                            } label: {
                                Label("Report to Agency", systemImage: "building.2")
                            }
                        } label: {
                            Image(systemName: "square.and.arrow.up")
                                .accessibilityLabel("Export and share options")
                        }
                    }
                }
            }
            .overlay {
                if isExporting {
                    ZStack {
                        Color.black.opacity(0.4)
                            .ignoresSafeArea()
                        VStack(spacing: 12) {
                            ProgressView()
                                .controlSize(.large)
                            Text("Generating report...")
                                .font(.subheadline)
                                .foregroundStyle(.white)
                        }
                        .padding(24)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    }
                }
            }
            .sheet(isPresented: $showShareSheet) {
                ShareSheet(items: shareItems)
            }
            .sheet(item: $agencyPickerItem) { item in
                AgencyPickerView(
                    agencies: item.agencies
                ) { agency in
                    prepareAgencyEmail(agency: agency)
                }
            }
            .sheet(isPresented: $showMailComposer) {
                if let agency = selectedAgency {
                    MailComposerView(
                        recipient: agency.email,
                        subject: agencyEmailSubject(),
                        body: agencyEmailBody(agency: agency),
                        attachmentURLs: [agencyPdfURL, agencyCsvURL].compactMap { $0 }
                    )
                }
            }
            .onChange(of: entries.count) {
                vm.update(with: entries)
            }
            .onAppear {
                vm.update(with: entries)
                locationManager.requestPermission()
            }
        }
    }

    // MARK: - Street Filter

    private var streetFilterSection: some View {
        Group {
            if vm.availableStreets.count > 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        streetFilterChip(label: "All Streets", street: nil)
                        ForEach(vm.availableStreets, id: \.self) { street in
                            streetFilterChip(label: street, street: street)
                        }
                    }
                }
            }
        }
    }

    private func streetFilterChip(label: String, street: String?) -> some View {
        let isSelected = vm.selectedStreet == street
        return Button {
            vm.selectStreet(street)
        } label: {
            Text(label)
                .font(.subheadline)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.accentColor : Color(.systemGray5))
                .foregroundStyle(isSelected ? .black : .primary)
                .clipShape(Capsule())
        }
    }

    // MARK: - Street Breakdown

    private var streetBreakdownSection: some View {
        StreetBreakdownView(
            streetGroups: vm.streetGroups,
            measurementSystem: measurementSystem,
            onSelectStreet: { vm.selectStreet($0) }
        )
    }

    // MARK: - V85 Section

    private var v85Section: some View {
        Group {
            if let stats = vm.stats {
                let mostCommonLimit = mostCommonSpeedLimit()
                V85CardView(v85: stats.v85, speedLimit: mostCommonLimit, system: measurementSystem)
            }
        }
    }

    // MARK: - Metrics Grid

    private var metricsGrid: some View {
        Group {
            if let stats = vm.stats {
                let unit = UnitConverter.speedUnit(measurementSystem)
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                ], spacing: 12) {
                    MetricCard(title: "Mean", value: String(format: "%.1f", UnitConverter.displaySpeed(stats.mean, system: measurementSystem)), unit: unit)
                    MetricCard(title: "Median", value: String(format: "%.1f", UnitConverter.displaySpeed(stats.median, system: measurementSystem)), unit: unit)
                    MetricCard(title: "Total", value: "\(stats.count)", unit: "entries")
                    MetricCard(
                        title: "Over Limit",
                        value: String(format: "%.0f%%", stats.overLimitPercent),
                        unit: "\(stats.overLimitCount) vehicles",
                        color: stats.overLimitPercent > 50 ? .red : .green
                    )
                }
            }
        }
    }

    // MARK: - Charts

    private var histogramChart: some View {
        HistogramChartView(histogram: vm.histogram)
    }

    private var hourlyChart: some View {
        HourlyChartView(hourlyAverages: vm.hourlyAverages, measurementSystem: measurementSystem)
    }

    private var scatterChart: some View {
        ScatterChartView(dailyEntries: vm.dailyEntries, measurementSystem: measurementSystem)
    }

    // MARK: - Helpers

    private func mostCommonSpeedLimit() -> Double {
        let active = vm.filteredEntries
        let limits = active.map(\.speedLimit)
        let counts = Dictionary(grouping: limits, by: { $0 }).mapValues(\.count)
        return counts.max(by: { $0.value < $1.value })?.key ?? RoadStandards.defaultSpeedLimit
    }

    private func exportCSV() {
        isExporting = true
        let capturedEntries = vm.filteredEntries
        let system = measurementSystem
        Task.detached {
            let url = ReportExporter.csvFileURL(entries: capturedEntries, system: system)
            await MainActor.run {
                isExporting = false
                if let url {
                    shareItems = [url]
                    showShareSheet = true
                }
            }
        }
    }

    private func exportPDF() {
        isExporting = true
        let capturedEntries = vm.filteredEntries
        let stats = vm.stats
        let system = measurementSystem
        Task.detached {
            let url = ReportExporter.pdfFileURL(entries: capturedEntries, stats: stats, system: system)
            await MainActor.run {
                isExporting = false
                if let url {
                    shareItems = [url]
                    showShareSheet = true
                }
            }
        }
    }

    // MARK: - Agency Reporting

    private func loadMatchedAgencies() {
        let active = vm.filteredEntries
        let located = active.filter { $0.latitude != nil && $0.longitude != nil }

        Task {
            let agencies = await resolveAgencies(located: located)
            agencyPickerItem = AgencyPickerItem(agencies: agencies)
        }
    }

    private func resolveAgencies(located: [SpeedEntry]) async -> [Agency] {
        let geocoder = CLGeocoder()

        // First: try entry coordinates
        if let coord = mostCommonCoordinate(from: located) {
            let location = CLLocation(latitude: coord.0, longitude: coord.1)
            if let agencies = await matchAgencies(geocoder: geocoder, location: location),
               !agencies.isEmpty {
                return agencies
            }
        }

        // Fallback: try current GPS location
        if let current = locationManager.currentLocation {
            if let agencies = await matchAgencies(geocoder: geocoder, location: current),
               !agencies.isEmpty {
                return agencies
            }
        }

        // No matches from either source — show all agencies
        return AgencyDirectory.load()
    }

    private func matchAgencies(geocoder: CLGeocoder, location: CLLocation) async -> [Agency]? {
        guard let placemark = try? await geocoder.reverseGeocodeLocation(location).first else {
            return nil
        }
        return AgencyDirectory.matching(
            city: placemark.locality,
            county: placemark.subAdministrativeArea,
            state: placemark.administrativeArea
        )
    }

    private func mostCommonCoordinate(from entries: [SpeedEntry]) -> (Double, Double)? {
        let coords = entries.compactMap { entry -> String? in
            guard let lat = entry.latitude, let lon = entry.longitude else { return nil }
            return "\(String(format: "%.3f", lat)),\(String(format: "%.3f", lon))"
        }
        guard let mostCommon = Dictionary(grouping: coords, by: { $0 })
            .max(by: { $0.value.count < $1.value.count })?.key else {
            return nil
        }
        let parts = mostCommon.split(separator: ",")
        guard parts.count == 2,
              let lat = Double(parts[0]),
              let lon = Double(parts[1]) else { return nil }
        return (lat, lon)
    }

    private func prepareAgencyEmail(agency: Agency) {
        selectedAgency = agency
        let capturedEntries = vm.filteredEntries
        let stats = vm.stats
        let system = measurementSystem
        isExporting = true
        Task.detached {
            let pdfURL = ReportExporter.pdfFileURL(entries: capturedEntries, stats: stats, system: system)
            let csvURL = ReportExporter.csvFileURL(entries: capturedEntries, system: system)
            await MainActor.run {
                agencyPdfURL = pdfURL
                agencyCsvURL = csvURL
                isExporting = false
                if MFMailComposeViewController.canSendMail() {
                    showMailComposer = true
                } else {
                    var items: [Any] = [agencyEmailBody(agency: agency)]
                    if let pdf = pdfURL { items.append(pdf) }
                    if let csv = csvURL { items.append(csv) }
                    shareItems = items
                    showShareSheet = true
                }
            }
        }
    }

    private func agencyEmailSubject() -> String {
        let active = vm.filteredEntries
        let street = mostCommonStreet(from: active)
        let limit = mostCommonSpeedLimit()
        let unit = UnitConverter.speedUnit(measurementSystem)
        if let stats = vm.stats {
            let v85Display = Int(UnitConverter.displaySpeed(stats.v85, system: measurementSystem))
            let limitDisplay = Int(UnitConverter.displaySpeed(limit, system: measurementSystem))
            return "Speeding Concern: \(street) \u{2014} V85 \(v85Display) \(unit) in a \(limitDisplay) \(unit) Zone"
        }
        return "Speeding Concern: \(street)"
    }

    private func agencyEmailBody(agency: Agency) -> String {
        let active = vm.filteredEntries
        let street = mostCommonStreet(from: active)
        let limit = mostCommonSpeedLimit()
        let unit = UnitConverter.speedUnit(measurementSystem).lowercased()
        guard let stats = vm.stats else { return "" }

        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium

        let timestamps = active.map(\.timestamp).sorted()
        let start = timestamps.first.map { dateFormatter.string(from: $0) } ?? "N/A"
        let end = timestamps.last.map { dateFormatter.string(from: $0) } ?? "N/A"

        let v85Display = String(format: "%.1f", UnitConverter.displaySpeed(stats.v85, system: measurementSystem))
        let meanDisplay = String(format: "%.1f", UnitConverter.displaySpeed(stats.mean, system: measurementSystem))
        let limitDisplay = Int(UnitConverter.displaySpeed(limit, system: measurementSystem))

        return """
        Dear \(agency.name),

        I am writing to report a speeding concern on \(street).

        Over \(stats.count) observations from \(start) to \(end):

        \u{2022} V85 Speed: \(v85Display) \(unit) (85th percentile)
        \u{2022} Average Speed: \(meanDisplay) \(unit)
        \u{2022} Speed Limit: \(limitDisplay) \(unit)
        \u{2022} Vehicles Over Limit: \(stats.overLimitCount) (\(String(format: "%.0f", stats.overLimitPercent))%)

        A detailed report is attached.

        Data collected with SlowThemDown (https://github.com/itsmeduncan/SlowThemDown).
        """
    }

    private func mostCommonStreet(from entries: [SpeedEntry]) -> String {
        let streets = entries.map(\.streetName).filter { !$0.isEmpty }
        let counts = Dictionary(grouping: streets, by: { $0 }).mapValues(\.count)
        return counts.max(by: { $0.value < $1.value })?.key ?? "Unknown Street"
    }

}

// MARK: - ShareSheet

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
