import Charts
import CoreLocation
import MessageUI
import SwiftData
import SwiftUI

struct ReportView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \SpeedEntry.timestamp, order: .reverse) private var entries: [SpeedEntry]
    @State private var vm = ReportViewModel()
    @State private var showShareSheet = false
    @State private var shareURL: URL?
    @State private var isExporting = false
    @State private var showingDemoData = SeedData.isSeeded
    @State private var showAgencyPicker = false
    @State private var showMailComposer = false
    @State private var selectedAgency: Agency?
    @State private var agencyPdfURL: URL?
    @State private var matchedAgencies: [Agency] = []

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
                            Text("Generating report…")
                                .font(.subheadline)
                                .foregroundStyle(.white)
                        }
                        .padding(24)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    }
                }
            }
            .sheet(isPresented: $showShareSheet) {
                if let url = shareURL {
                    ShareSheet(items: [url])
                }
            }
            .sheet(isPresented: $showAgencyPicker) {
                AgencyPickerView(
                    agencies: matchedAgencies
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
                        attachmentURL: agencyPdfURL
                    )
                }
            }
            .onChange(of: entries.count) {
                vm.update(with: entries)
            }
            .onAppear {
                vm.update(with: entries)
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
        VStack(alignment: .leading, spacing: 12) {
            Text("By Street")
                .font(.headline)

            ForEach(vm.streetGroups) { group in
                Button {
                    vm.selectStreet(group.name)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(group.name)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Text("\(group.count) entries · Avg \(String(format: "%.1f", group.meanSpeed)) MPH")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(String(format: "%.0f%%", group.overLimitPercent))
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(group.overLimitPercent > 50 ? .red : .green)
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .background(Color(.systemGray6).opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - V85 Section

    private var v85Section: some View {
        Group {
            if let stats = vm.stats {
                let mostCommonLimit = mostCommonSpeedLimit()
                V85CardView(v85: stats.v85, speedLimit: mostCommonLimit)
            }
        }
    }

    // MARK: - Metrics Grid

    private var metricsGrid: some View {
        Group {
            if let stats = vm.stats {
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                ], spacing: 12) {
                    MetricCard(title: "Mean", value: String(format: "%.1f", stats.mean), unit: "MPH")
                    MetricCard(title: "Median", value: String(format: "%.1f", stats.median), unit: "MPH")
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
        VStack(alignment: .leading, spacing: 8) {
            Text("Speed Distribution")
                .font(.headline)
            Chart(vm.histogram) { bucket in
                BarMark(
                    x: .value("Speed Range", bucket.range),
                    y: .value("Count", bucket.count)
                )
                .foregroundStyle(Color.accentColor.gradient)
            }
            .frame(height: 200)
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var hourlyChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Average Speed by Hour")
                .font(.headline)
            Chart(vm.hourlyAverages) { item in
                LineMark(
                    x: .value("Hour", item.hourLabel),
                    y: .value("Avg Speed", item.averageSpeed)
                )
                .foregroundStyle(Color.orange)
                .interpolationMethod(.catmullRom)

                PointMark(
                    x: .value("Hour", item.hourLabel),
                    y: .value("Avg Speed", item.averageSpeed)
                )
                .foregroundStyle(Color.orange)
            }
            .frame(height: 200)
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var scatterChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Speeds Over Time")
                .font(.headline)
            Chart(vm.dailyEntries) { item in
                PointMark(
                    x: .value("Date", item.date),
                    y: .value("Speed", item.speed)
                )
                .foregroundStyle(Color.blue)
            }
            .frame(height: 200)
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Helpers

    private func mostCommonSpeedLimit() -> Int {
        let active = vm.filteredEntries
        let limits = active.map(\.speedLimit)
        let counts = Dictionary(grouping: limits, by: { $0 }).mapValues(\.count)
        return counts.max(by: { $0.value < $1.value })?.key ?? RoadStandards.defaultSpeedLimit
    }

    private func exportCSV() {
        isExporting = true
        let capturedEntries = vm.filteredEntries
        Task.detached {
            let url = ReportExporter.csvFileURL(entries: capturedEntries)
            await MainActor.run {
                isExporting = false
                if let url {
                    shareURL = url
                    showShareSheet = true
                }
            }
        }
    }

    private func exportPDF() {
        isExporting = true
        let capturedEntries = vm.filteredEntries
        let stats = vm.stats
        Task.detached {
            let url = ReportExporter.pdfFileURL(entries: capturedEntries, stats: stats)
            await MainActor.run {
                isExporting = false
                if let url {
                    shareURL = url
                    showShareSheet = true
                }
            }
        }
    }

    // MARK: - Agency Reporting

    private func loadMatchedAgencies() {
        let active = vm.filteredEntries
        let located = active.filter { $0.latitude != nil && $0.longitude != nil }

        if let coord = mostCommonCoordinate(from: located) {
            Task {
                let location = CLLocation(latitude: coord.0, longitude: coord.1)
                let geocoder = CLGeocoder()
                if let placemark = try? await geocoder.reverseGeocodeLocation(location).first {
                    let city = placemark.locality
                    let county = placemark.subAdministrativeArea
                    let state = placemark.administrativeArea
                    matchedAgencies = AgencyDirectory.matching(city: city, county: county, state: state)
                } else {
                    matchedAgencies = AgencyDirectory.load()
                }
                showAgencyPicker = true
            }
        } else {
            // No coordinates on entries — show all agencies
            matchedAgencies = AgencyDirectory.load()
            showAgencyPicker = true
        }
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
        isExporting = true
        Task.detached {
            let url = ReportExporter.pdfFileURL(entries: capturedEntries, stats: stats)
            await MainActor.run {
                agencyPdfURL = url
                isExporting = false
                if MFMailComposeViewController.canSendMail() {
                    showMailComposer = true
                } else {
                    openMailtoFallback(agency: agency)
                }
            }
        }
    }

    private func agencyEmailSubject() -> String {
        let active = vm.filteredEntries
        let street = mostCommonStreet(from: active)
        let limit = mostCommonSpeedLimit()
        if let stats = vm.stats {
            return "Speeding Concern: \(street) — V85 \(String(format: "%.0f", stats.v85)) MPH in a \(limit) MPH Zone"
        }
        return "Speeding Concern: \(street)"
    }

    private func agencyEmailBody(agency: Agency) -> String {
        let active = vm.filteredEntries
        let street = mostCommonStreet(from: active)
        let limit = mostCommonSpeedLimit()
        guard let stats = vm.stats else { return "" }

        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium

        let timestamps = active.map(\.timestamp).sorted()
        let start = timestamps.first.map { dateFormatter.string(from: $0) } ?? "N/A"
        let end = timestamps.last.map { dateFormatter.string(from: $0) } ?? "N/A"

        return """
        Dear \(agency.name),

        I am writing to report a speeding concern on \(street).

        Over \(stats.count) observations from \(start) to \(end):

        • V85 Speed: \(String(format: "%.1f", stats.v85)) MPH (85th percentile)
        • Average Speed: \(String(format: "%.1f", stats.mean)) MPH
        • Speed Limit: \(limit) MPH
        • Vehicles Over Limit: \(stats.overLimitCount) (\(String(format: "%.0f", stats.overLimitPercent))%)

        A detailed report is attached.

        Data collected with SlowThemDown (https://github.com/itsmeduncan/SlowThemDown).
        """
    }

    private func mostCommonStreet(from entries: [SpeedEntry]) -> String {
        let streets = entries.map(\.streetName).filter { !$0.isEmpty }
        let counts = Dictionary(grouping: streets, by: { $0 }).mapValues(\.count)
        return counts.max(by: { $0.value < $1.value })?.key ?? "Unknown Street"
    }

    private func openMailtoFallback(agency: Agency) {
        let subject = agencyEmailSubject().addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let body = agencyEmailBody(agency: agency).addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        if let url = URL(string: "mailto:\(agency.email)?subject=\(subject)&body=\(body)") {
            UIApplication.shared.open(url)
        }
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
