import SwiftUI
import UIKit

func relativeTime(_ date: Date) -> String {
    RelativeDateTimeFormatter().localizedString(for: date, relativeTo: .now)
}

func initials(_ name: String) -> String {
    let parts = name.split(separator: " ").prefix(2)
    let letters = parts.compactMap { $0.first }.map { String($0).uppercased() }.joined()
    return letters.isEmpty ? "?" : letters
}

func telURL(_ number: String?) -> URL? {
    guard let number else { return nil }
    let cleaned = number.filter { $0.isNumber || $0 == "+" }
    guard !cleaned.isEmpty else { return nil }
    return URL(string: "tel://\(cleaned)")
}

func instagramURL(_ username: String?) -> URL? {
    guard let username, !username.isEmpty else { return nil }
    return URL(string: "https://instagram.com/\(username)")
}

struct AvatarView: View {
    let person: Person
    var size: CGFloat = 44

    var body: some View {
        if let data = person.photoData, let image = UIImage(data: data) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(width: size, height: size)
                .clipShape(Circle())
        } else {
            ZStack {
                Circle().fill(Color.accentColor.opacity(0.15))
                Text(initials(person.displayName))
                    .font(.system(size: size * 0.4, weight: .semibold))
                    .foregroundStyle(Color.accentColor)
            }
            .frame(width: size, height: size)
        }
    }
}

struct StatTile: View {
    let value: String
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value).font(.title2.bold())
            Text(label).font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }
}

struct LabeledProgress: View {
    let done: Int
    let total: Int
    let caption: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ProgressView(value: total == 0 ? 0 : Double(done) / Double(total))
            Text(caption).font(.caption).foregroundStyle(.secondary)
        }
    }
}

struct Chip: View {
    let label: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    Capsule().fill(selected ? Color.accentColor.opacity(0.2) : Color(.secondarySystemBackground))
                )
                .overlay(
                    Capsule().strokeBorder(selected ? Color.accentColor : .clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}

private struct CardBackground: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
    }
}

extension View {
    func card() -> some View { modifier(CardBackground()) }
}
