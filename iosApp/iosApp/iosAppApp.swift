import SwiftUI

@main
struct MangaNaviApp: App {
    @StateObject private var adService = AdService.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(adService)
        }
    }
}
