import SwiftUI
import WebKit

struct YouTubePlayerView: UIViewRepresentable {
    let videoId: String
    let videoUrl: String?
    @Binding var loadError: String?

    init(videoId: String, videoUrl: String? = nil, loadError: Binding<String?>) {
        self.videoId = videoId
        self.videoUrl = videoUrl
        self._loadError = loadError
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(loadError: $loadError)
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        if #available(iOS 16.0, *) {
            config.mediaTypesRequiringUserActionForPlayback = []
        } else {
            config.requiresUserActionForMediaPlayback = false
        }

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.scrollView.isScrollEnabled = true
        webView.backgroundColor = .black
        webView.isOpaque = false
        webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

        load(in: webView)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        let expectedUrl = resolvedUrl()
        if uiView.url?.absoluteString != expectedUrl {
            load(in: uiView)
        }
    }

    private func resolvedUrl() -> String {
        if let videoUrl = videoUrl, !videoUrl.isEmpty {
            return videoUrl
        }
        return "https://m.youtube.com/watch?v=\(videoId)"
    }

    private func load(in webView: WKWebView) {
        loadError = nil
        if let url = URL(string: resolvedUrl()) {
            webView.load(URLRequest(url: url))
        } else {
            loadError = l10n("invalid_video_url")
        }
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        @Binding var loadError: String?

        init(loadError: Binding<String?>) {
            self._loadError = loadError
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            loadError = error.localizedDescription
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            loadError = error.localizedDescription
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            loadError = nil
        }
    }
}
