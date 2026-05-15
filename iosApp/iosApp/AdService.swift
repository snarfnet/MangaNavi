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

    @Published private(set) var didCompleteTrackingFlow = false
    @Published private(set) var isReady = false

    private var didStart = false

    private init() {}

    func prepareForFirstLaunch() {
        guard !didCompleteTrackingFlow else { return }

        guard #available(iOS 14.5, *) else {
            didCompleteTrackingFlow = true
            startAdsIfNeeded()
            return
        }

        if ATTrackingManager.trackingAuthorizationStatus == .notDetermined {
            return
        }

        didCompleteTrackingFlow = true
        startAdsIfNeeded()
    }

    func requestTrackingAuthorizationFromGate() async {
        guard #available(iOS 14.5, *) else {
            didCompleteTrackingFlow = true
            startAdsIfNeeded()
            return
        }

        guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined else {
            didCompleteTrackingFlow = true
            startAdsIfNeeded()
            return
        }

        await withCheckedContinuation { continuation in
            ATTrackingManager.requestTrackingAuthorization { _ in
                continuation.resume()
            }
        }

        didCompleteTrackingFlow = true
        startAdsIfNeeded()
    }

    func startAdsIfNeeded() {
        guard !didStart else { return }
        didStart = true
        GADMobileAds.sharedInstance().start(completionHandler: nil)
        isReady = true
    }
}

struct AdMobBannerSlotView: View {
    let placement: AdPlacement
    @EnvironmentObject private var adService: AdService

    var body: some View {
        if adService.isReady {
            BannerViewContainer(
                adSize: GADAdSizeBanner,
                adUnitID: AdConfiguration.bannerUnitID(for: placement)
            )
            .frame(width: GADAdSizeBanner.size.width, height: GADAdSizeBanner.size.height)
            .frame(maxWidth: .infinity)
            .padding(.top, 6)
            .background(Color(red: 0.96, green: 0.94, blue: 0.89))
            .accessibilityLabel("広告")
        }
    }
}

private struct BannerViewContainer: UIViewRepresentable {
    let adSize: GADAdSize
    let adUnitID: String

    func makeUIView(context: Context) -> GADBannerView {
        let banner = GADBannerView(adSize: adSize)
        banner.adUnitID = adUnitID
        banner.rootViewController = UIApplication.shared.adRootViewController
        banner.load(GADRequest())
        return banner
    }

    func updateUIView(_ banner: GADBannerView, context: Context) {
        if banner.adUnitID != adUnitID {
            banner.adUnitID = adUnitID
            banner.load(GADRequest())
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
