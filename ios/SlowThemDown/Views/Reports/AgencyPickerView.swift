import MessageUI
import SwiftUI

struct AgencyPickerView: View {
    let agencies: [Agency]
    let onSelect: (Agency) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if agencies.isEmpty {
                    ContentUnavailableView(
                        "No Agencies Found",
                        systemImage: "building.2",
                        description: Text(
                            "No agencies match this location yet. "
                            + "You can contribute one at github.com/itsmeduncan/SlowThemDown."
                        )
                    )
                } else {
                    List(agencies) { agency in
                        Button {
                            onSelect(agency)
                            dismiss()
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(agency.name)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                Text(agency.email)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                if let notes = agency.notes {
                                    Text(notes)
                                        .font(.caption2)
                                        .foregroundStyle(.tertiary)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle("Report to Agency")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

struct MailComposerView: UIViewControllerRepresentable {
    let recipient: String
    let subject: String
    let body: String
    let attachmentURL: URL?
    @Environment(\.dismiss) private var dismiss

    func makeCoordinator() -> Coordinator {
        Coordinator(dismiss: dismiss)
    }

    func makeUIViewController(context: Context) -> MFMailComposeViewController {
        let vc = MFMailComposeViewController()
        vc.mailComposeDelegate = context.coordinator
        vc.setToRecipients([recipient])
        vc.setSubject(subject)
        vc.setMessageBody(body, isHTML: false)
        if let url = attachmentURL,
           let data = try? Data(contentsOf: url) {
            vc.addAttachmentData(data, mimeType: "application/pdf", fileName: url.lastPathComponent)
        }
        return vc
    }

    func updateUIViewController(_ uiViewController: MFMailComposeViewController, context: Context) {}

    class Coordinator: NSObject, MFMailComposeViewControllerDelegate {
        let dismiss: DismissAction

        init(dismiss: DismissAction) {
            self.dismiss = dismiss
        }

        func mailComposeController(
            _ controller: MFMailComposeViewController,
            didFinishWith result: MFMailComposeResult,
            error: Error?
        ) {
            dismiss()
        }
    }
}
