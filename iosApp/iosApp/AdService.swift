import AppTrackingTransparency
import GoogleMobileAds
import SwiftUI
import UIKit

enum AdPlacement {
    case homeBottom
}

struct AdConfiguration {
    static let appID = "ca-app-pub-9404799280370656~8948872604"
    static let primaryBannerUnitID = "ca-app-pub-9404799280370656/2700982853"
    static let secondaryBannerUnitID = "ca-app-pub-9404799280370656/5009627599"
    static let tertiaryBannerUnitID = "ca-app-pub-9404799280370656/6705852641"

    static func bannerUnitID(for placement: AdPlacement) -> String {
        switch placement {
        case .homeBottom:
            primaryBannerUnitID
        }
    }
}

@MainActor
final class AdService: ObservableObject {
    static let shared = AdService()

    @Published private(set) var isReady = false
    private var didStart = false

    private init() {}

    func start() async {
        guard !didStart else { return }
        didStart = true

        await requestTrackingAuthorizationIfNeeded()
        await MobileAds.shared.start()
        isReady = true
    }

    private func requestTrackingAuthorizationIfNeeded() async {
        guard #available(iOS 14.5, *),
              ATTrackingManager.trackingAuthorizationStatus == .notDetermined else {
            return
        }

        _ = await withCheckedContinuation { continuation in
            ATTrackingManager.requestTrackingAuthorization { status in
                continuation.resume(returning: status)
            }
        }
    }
}

struct AdMobBannerSlotView: View {
    let placement: AdPlacement
    @EnvironmentObject private var adService: AdService

    var body: some View {
        if adService.isReady {
            BannerViewContainer(
                adSize: AdSizeBanner,
                adUnitID: AdConfiguration.bannerUnitID(for: placement)
            )
            .frame(width: AdSizeBanner.size.width, height: AdSizeBanner.size.height)
            .frame(maxWidth: .infinity)
            .padding(.top, 6)
            .background(AppPalette.paper)
            .accessibilityLabel("広告")
        }
    }
}

private struct BannerViewContainer: UIViewRepresentable {
    let adSize: AdSize
    let adUnitID: String

    func makeUIView(context: Context) -> BannerView {
        let banner = BannerView(adSize: adSize)
        banner.adUnitID = adUnitID
        banner.rootViewController = UIApplication.shared.adRootViewController
        banner.load(Request())
        return banner
    }

    func updateUIView(_ banner: BannerView, context: Context) {
        if banner.adUnitID != adUnitID {
            banner.adUnitID = adUnitID
            banner.load(Request())
        }
    }
}

private extension UIApplication {
    var adRootViewController: UIViewController? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController?
            .topPresentedViewController
    }
}

private extension UIViewController {
    var topPresentedViewController: UIViewController {
        presentedViewController?.topPresentedViewController ?? self
    }
}
