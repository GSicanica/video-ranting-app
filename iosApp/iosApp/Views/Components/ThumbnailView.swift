import SwiftUI

struct ThumbnailView: View {
    let urlString: String
    var aspectRatio: CGFloat = 16 / 9
    var cornerRadius: CGFloat = 12

    private var resolvedUrl: URL? {
        let trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let url = URL(string: trimmed) else { return nil }
        if url.host == "via.placeholder.com" { return nil }
        return url
    }

    var body: some View {
        Group {
            if let url = resolvedUrl {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .aspectRatio(aspectRatio, contentMode: .fill)
                } placeholder: {
                    Placeholder()
                }
            } else {
                Placeholder()
            }
        }
        .cornerRadius(cornerRadius)
        .clipped()
    }
}

private struct Placeholder: View {
    var body: some View {
        Rectangle()
            .fill(Color.gray.opacity(0.25))
            .overlay(
                Image(systemName: "photo")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color.white.opacity(0.6))
            )
            .aspectRatio(16 / 9, contentMode: .fill)
    }
}

