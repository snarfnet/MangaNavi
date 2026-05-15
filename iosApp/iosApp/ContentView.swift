import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var adService: AdService

    var body: some View {
        Group {
            if adService.didCompleteTrackingFlow {
                MangaNaviHomeView()
                    .task {
                        adService.startAdsIfNeeded()
                    }
            } else {
                TrackingIntroView {
                    Task {
                        await adService.requestTrackingAuthorizationFromGate()
                    }
                }
            }
        }
        .task {
            adService.prepareForFirstLaunch()
        }
    }
}

private struct TrackingIntroView: View {
    let action: () -> Void

    var body: some View {
        ZStack {
            AppPalette.paper.ignoresSafeArea()

            VStack(spacing: 22) {
                Spacer(minLength: 20)

                Image(systemName: "book.pages.fill")
                    .font(.system(size: 58, weight: .black))
                    .foregroundStyle(AppPalette.crimson)

                VStack(spacing: 10) {
                    Text("MangaNavi")
                        .font(.system(size: 42, weight: .black, design: .serif))
                        .foregroundStyle(AppPalette.ink)

                    Text("広告の表示と効果測定について確認します")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(AppPalette.ink.opacity(0.72))
                        .multilineTextAlignment(.center)
                }

                VStack(alignment: .leading, spacing: 14) {
                    PermissionPoint(icon: "magnifyingglass", text: "許可しなくても、検索・ジャンル絞り込み・読書リストはそのまま使えます。")
                    PermissionPoint(icon: "hand.raised.fill", text: "次の画面でiOS標準のApp Tracking Transparency確認が表示されます。")
                    PermissionPoint(icon: "rectangle.3.group", text: "広告SDKと広告枠は、この確認が終わった後に読み込みます。")
                }
                .padding(18)
                .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                .frame(maxWidth: 540)

                Button(action: action) {
                    Label("続ける", systemImage: "arrow.right.circle.fill")
                        .primaryActionStyle()
                }
                .buttonStyle(.plain)
                .frame(maxWidth: 540)

                Spacer(minLength: 20)
            }
            .padding(24)
        }
    }
}

private struct PermissionPoint: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.headline.weight(.black))
                .foregroundStyle(AppPalette.crimson)
                .frame(width: 26)

            Text(text)
                .font(.body.weight(.semibold))
                .foregroundStyle(AppPalette.ink.opacity(0.78))
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct MangaNaviHomeView: View {
    @State private var selectedManga: MangaPick?
    @State private var selectedTab = 0
    @State private var searchText = ""
    @State private var selectedGenre: String?
    @AppStorage("savedMangaTitles") private var savedMangaTitles = ""
    @AppStorage("finishedMangaTitles") private var finishedMangaTitles = ""

    private let picks = MangaPick.samples

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $selectedTab) {
                home
                    .tabItem { Label("ホーム", systemImage: "book.pages") }
                    .tag(0)

                SearchAndGenreView(
                    allPicks: picks,
                    allGenres: genres,
                    searchText: $searchText,
                    selectedGenre: $selectedGenre,
                    selectedManga: $selectedManga,
                    savedTitles: savedSet,
                    finishedTitles: finishedSet
                )
                .tabItem { Label("探す", systemImage: "magnifyingglass") }
                .tag(1)

                ReadingListView(
                    picks: picks,
                    selectedManga: $selectedManga,
                    savedTitles: savedSet,
                    finishedTitles: finishedSet
                )
                .tabItem { Label("リスト", systemImage: "bookmark.fill") }
                .tag(2)
            }
            .tint(AppPalette.ink)

            AdMobBannerSlotView(placement: .homeBottom)
        }
        .sheet(item: $selectedManga) { manga in
            MangaDetailView(
                manga: manga,
                isSaved: savedSet.contains(manga.title),
                isFinished: finishedSet.contains(manga.title),
                toggleSaved: { savedMangaTitles = toggled(savedMangaTitles, title: manga.title) },
                toggleFinished: { finishedMangaTitles = toggled(finishedMangaTitles, title: manga.title) }
            )
        }
    }

    private var home: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 22) {
                    HeroPanel(featured: dailyPick) {
                        selectedManga = dailyPick
                    }

                    HStack(spacing: 10) {
                        MiniMetric(title: "保存", value: "\(savedSet.count)")
                        MiniMetric(title: "読了", value: "\(finishedSet.count)")
                        MiniMetric(title: "作品", value: "\(picks.count)")
                    }

                    SectionHeader(title: "すぐ使える機能", action: nil)
                    VStack(spacing: 10) {
                        HomeShortcut(title: "作品名・ジャンル・気分で検索", systemImage: "magnifyingglass") {
                            selectedTab = 1
                        }
                        HomeShortcut(title: "読みたい作品を保存", systemImage: "bookmark.fill") {
                            selectedTab = 2
                        }
                        HomeShortcut(title: "読了作品を管理", systemImage: "checkmark.seal.fill") {
                            selectedTab = 2
                        }
                    }

                    SectionHeader(title: "ジャンルから探す", action: "すべて見る")
                        .onTapGesture {
                            selectedGenre = nil
                            selectedTab = 1
                        }

                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 130), spacing: 10)], spacing: 10) {
                        ForEach(genres.prefix(8), id: \.self) { genre in
                            GenreButton(title: genre, count: picks.filter { $0.genre == genre }.count, isSelected: false) {
                                selectedGenre = genre
                                selectedTab = 1
                            }
                        }
                    }

                    SectionHeader(title: "今日の候補", action: "ランダム")
                        .onTapGesture {
                            selectedManga = picks.randomElement() ?? dailyPick
                        }

                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(Array(picks.prefix(8))) { manga in
                            Button {
                                selectedManga = manga
                            } label: {
                                MangaCard(
                                    manga: manga,
                                    isSaved: savedSet.contains(manga.title),
                                    isFinished: finishedSet.contains(manga.title)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 14)
                .padding(.bottom, 28)
            }
            .background(AppPalette.paper.ignoresSafeArea())
            .navigationTitle("MangaNavi")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var columns: [GridItem] {
        [GridItem(.adaptive(minimum: 160, maximum: 230), spacing: 14)]
    }

    private var genres: [String] {
        Array(Set(picks.map(\.genre))).sorted()
    }

    private var savedSet: Set<String> {
        Set(savedMangaTitles.split(separator: "|").map(String.init))
    }

    private var finishedSet: Set<String> {
        Set(finishedMangaTitles.split(separator: "|").map(String.init))
    }

    private var dailyPick: MangaPick {
        picks[Calendar.current.component(.day, from: Date()) % picks.count]
    }

    private func toggled(_ storage: String, title: String) -> String {
        var values = Set(storage.split(separator: "|").map(String.init))
        if values.contains(title) {
            values.remove(title)
        } else {
            values.insert(title)
        }
        return values.sorted().joined(separator: "|")
    }
}

private struct SearchAndGenreView: View {
    let allPicks: [MangaPick]
    let allGenres: [String]
    @Binding var searchText: String
    @Binding var selectedGenre: String?
    @Binding var selectedManga: MangaPick?
    let savedTitles: Set<String>
    let finishedTitles: Set<String>

    private var filteredPicks: [MangaPick] {
        allPicks.filter { manga in
            let matchesGenre = selectedGenre == nil || manga.genre == selectedGenre
            let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
            let matchesSearch = query.isEmpty ||
                manga.title.localizedCaseInsensitiveContains(query) ||
                manga.genre.localizedCaseInsensitiveContains(query) ||
                manga.mood.localizedCaseInsensitiveContains(query) ||
                manga.pitch.localizedCaseInsensitiveContains(query) ||
                manga.note.localizedCaseInsensitiveContains(query)
            return matchesGenre && matchesSearch
        }
    }

    private var columns: [GridItem] {
        [GridItem(.adaptive(minimum: 150, maximum: 220), spacing: 12)]
    }

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 20) {
                    TextField("作品名・ジャンル・気分で検索", text: $searchText)
                        .textInputAutocapitalization(.never)
                        .disableAutocorrection(true)
                        .padding(16)
                        .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .stroke(AppPalette.ink.opacity(0.08), lineWidth: 1)
                        )

                    HStack {
                        SectionHeader(title: "ジャンルを選ぶ", action: selectedGenre == nil ? nil : "解除")
                            .onTapGesture {
                                withAnimation(.snappy) { selectedGenre = nil }
                            }
                    }

                    LazyVGrid(columns: columns, spacing: 12) {
                        GenreButton(title: "すべて", count: allPicks.count, isSelected: selectedGenre == nil) {
                            withAnimation(.snappy) { selectedGenre = nil }
                        }

                        ForEach(allGenres, id: \.self) { genre in
                            GenreButton(
                                title: genre,
                                count: allPicks.filter { $0.genre == genre }.count,
                                isSelected: selectedGenre == genre
                            ) {
                                withAnimation(.snappy) {
                                    selectedGenre = selectedGenre == genre ? nil : genre
                                }
                            }
                        }
                    }

                    ResultSummary(
                        selectedGenre: selectedGenre,
                        resultCount: filteredPicks.count,
                        totalCount: allPicks.count
                    )

                    if filteredPicks.isEmpty {
                        EmptyListText(text: "条件に合う作品がありません。ジャンル解除や検索語の変更を試してください。")
                    } else {
                        LazyVGrid(columns: columns, spacing: 14) {
                            ForEach(filteredPicks) { manga in
                                Button {
                                    selectedManga = manga
                                } label: {
                                    MangaCard(
                                        manga: manga,
                                        isSaved: savedTitles.contains(manga.title),
                                        isFinished: finishedTitles.contains(manga.title)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(AppPalette.paper.ignoresSafeArea())
            .navigationTitle("探す")
        }
    }
}

private struct HeroPanel: View {
    let featured: MangaPick
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack(alignment: .bottomLeading) {
                LinearGradient(
                    colors: [AppPalette.ink, AppPalette.crimson, AppPalette.gold],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )

                MangaTonePattern()
                    .opacity(0.22)

                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Label("今日の一冊", systemImage: "sparkles")
                            .font(.caption.weight(.bold))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(.white.opacity(0.16), in: Capsule())

                        Spacer()

                        Text(featured.score)
                            .font(.callout.monospacedDigit().weight(.black))
                            .padding(.horizontal, 11)
                            .padding(.vertical, 7)
                            .background(AppPalette.paper, in: Capsule())
                            .foregroundStyle(AppPalette.ink)
                    }

                    Spacer(minLength: 44)

                    VStack(alignment: .leading, spacing: 8) {
                        Text(featured.title)
                            .font(.system(size: 32, weight: .black, design: .serif))
                            .lineLimit(2)
                            .minimumScaleFactor(0.76)

                        Text(featured.pitch)
                            .font(.subheadline.weight(.semibold))
                            .lineLimit(2)
                            .foregroundStyle(.white.opacity(0.86))
                    }
                }
                .foregroundStyle(.white)
                .padding(22)
            }
            .frame(minHeight: 260)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .shadow(color: AppPalette.ink.opacity(0.18), radius: 18, y: 12)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("今日のおすすめ \(featured.title)")
    }
}

private struct HomeShortcut: View {
    let title: String
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: systemImage)
                    .font(.headline.weight(.black))
                    .frame(width: 34, height: 34)
                    .background(AppPalette.crimson.opacity(0.12), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .foregroundStyle(AppPalette.crimson)

                Text(title)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(AppPalette.ink)

                Spacer()

                Image(systemName: "chevron.right")
                    .foregroundStyle(AppPalette.ink.opacity(0.35))
            }
            .padding(14)
            .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct ReadingListView: View {
    let picks: [MangaPick]
    @Binding var selectedManga: MangaPick?
    let savedTitles: Set<String>
    let finishedTitles: Set<String>

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    ReadingGroup(title: "保存した作品") {
                        listRows(for: picks.filter { savedTitles.contains($0.title) }, emptyText: "作品詳細から「読みたい」を押すとここに並びます。")
                    }

                    ReadingGroup(title: "読了した作品") {
                        listRows(for: picks.filter { finishedTitles.contains($0.title) }, emptyText: "読み終えた作品を記録できます。")
                    }

                    ReadingGroup(title: "次に読む候補") {
                        listRows(for: picks.filter { !finishedTitles.contains($0.title) }.prefix(6).map { $0 }, emptyText: "")
                    }
                }
                .padding(20)
            }
            .background(AppPalette.paper.ignoresSafeArea())
            .navigationTitle("読書リスト")
        }
    }

    private func listRows(for rows: [MangaPick], emptyText: String) -> some View {
        VStack(spacing: 12) {
            if rows.isEmpty {
                EmptyListText(text: emptyText)
            } else {
                ForEach(rows) { manga in
                    Button {
                        selectedManga = manga
                    } label: {
                        HStack(spacing: 14) {
                            CoverMark(manga: manga)
                                .frame(width: 70, height: 96)

                            VStack(alignment: .leading, spacing: 7) {
                                Text(manga.title)
                                    .font(.headline)
                                    .foregroundStyle(AppPalette.ink)

                                Text(manga.note)
                                    .font(.subheadline)
                                    .lineLimit(2)
                                    .foregroundStyle(AppPalette.ink.opacity(0.66))
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .foregroundStyle(AppPalette.ink.opacity(0.35))
                        }
                        .padding(14)
                        .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct MangaCard: View {
    let manga: MangaPick
    let isSaved: Bool
    let isFinished: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            CoverMark(manga: manga)
                .aspectRatio(0.72, contentMode: .fit)

            VStack(alignment: .leading, spacing: 5) {
                Text(manga.title)
                    .font(.headline.weight(.bold))
                    .lineLimit(2)
                    .foregroundStyle(AppPalette.ink)

                HStack {
                    Text(manga.genre)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(AppPalette.crimson)

                    Spacer()

                    Text(manga.score)
                        .font(.caption.monospacedDigit().weight(.black))
                        .foregroundStyle(AppPalette.ink.opacity(0.7))
                }
            }

            HStack(spacing: 6) {
                if isSaved {
                    StatusBadge(title: "保存済み", systemImage: "bookmark.fill")
                }
                if isFinished {
                    StatusBadge(title: "読了", systemImage: "checkmark.seal.fill")
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
        .shadow(color: AppPalette.ink.opacity(0.08), radius: 10, y: 6)
    }
}

private struct CoverMark: View {
    let manga: MangaPick

    var body: some View {
        ZStack {
            LinearGradient(colors: manga.colors, startPoint: .topLeading, endPoint: .bottomTrailing)

            MangaTonePattern()
                .opacity(0.24)

            VStack(alignment: .leading) {
                Text(manga.genre)
                    .font(.caption2.weight(.black))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(.white.opacity(0.22), in: Capsule())

                Spacer()

                Text(manga.shortTitle)
                    .font(.system(size: 22, weight: .black, design: .serif))
                    .lineSpacing(4)
                    .minimumScaleFactor(0.7)
            }
            .foregroundStyle(.white)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct MangaDetailView: View {
    let manga: MangaPick
    let isSaved: Bool
    let isFinished: Bool
    let toggleSaved: () -> Void
    let toggleFinished: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    CoverMark(manga: manga)
                        .frame(height: 260)

                    VStack(alignment: .leading, spacing: 10) {
                        Text(manga.title)
                            .font(.title.weight(.black))
                            .foregroundStyle(AppPalette.ink)

                        Text(manga.pitch)
                            .font(.headline.weight(.semibold))
                            .foregroundStyle(AppPalette.ink.opacity(0.72))
                    }

                    HStack(spacing: 10) {
                        InfoPill(title: "ジャンル", value: manga.genre)
                        InfoPill(title: "評価", value: manga.score)
                        InfoPill(title: "巻数", value: manga.volume)
                    }

                    Text(manga.note)
                        .font(.body.weight(.semibold))
                        .lineSpacing(5)
                        .foregroundStyle(AppPalette.ink.opacity(0.76))
                        .padding(16)
                        .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                    VStack(spacing: 10) {
                        Button(action: toggleSaved) {
                            Label(isSaved ? "読みたいから外す" : "読みたいに保存", systemImage: isSaved ? "bookmark.slash.fill" : "bookmark.fill")
                                .secondaryActionStyle(isActive: isSaved)
                        }

                        Button(action: toggleFinished) {
                            Label(isFinished ? "読了を取り消す" : "読了にする", systemImage: isFinished ? "arrow.uturn.backward.circle.fill" : "checkmark.seal.fill")
                                .secondaryActionStyle(isActive: isFinished)
                        }
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
            }
            .background(AppPalette.paper.ignoresSafeArea())
            .navigationTitle("作品詳細")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                Button("閉じる") { dismiss() }
                    .foregroundStyle(AppPalette.crimson)
            }
        }
    }
}

private struct ReadingGroup<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: title, action: nil)
            content()
        }
    }
}

private struct EmptyListText: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(AppPalette.ink.opacity(0.58))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct MiniMetric: View {
    let title: String
    let value: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title3.monospacedDigit().weight(.black))
                .foregroundStyle(AppPalette.ink)
            Text(title)
                .font(.caption.weight(.bold))
                .foregroundStyle(AppPalette.ink.opacity(0.58))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct SectionHeader: View {
    let title: String
    let action: String?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title)
                .font(.title2.weight(.black))
                .foregroundStyle(AppPalette.ink)

            Spacer()

            if let action {
                Text(action)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(AppPalette.crimson)
            }
        }
    }
}

private struct GenreButton: View {
    let title: String
    let count: Int
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.headline.weight(.black))

                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.headline.weight(.bold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)
                    Text("\(count)作品")
                        .font(.caption.weight(.bold))
                        .opacity(0.72)
                }

                Spacer(minLength: 0)
            }
            .foregroundStyle(isSelected ? .white : AppPalette.ink)
            .padding(.horizontal, 14)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 64)
            .background(isSelected ? AppPalette.crimson : .white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(isSelected ? AppPalette.crimson : AppPalette.ink.opacity(0.08), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .contentShape(Rectangle())
    }
}

private struct ResultSummary: View {
    let selectedGenre: String?
    let resultCount: Int
    let totalCount: Int

    var body: some View {
        HStack {
            Label(selectedGenre ?? "すべてのジャンル", systemImage: "line.3.horizontal.decrease.circle.fill")
                .font(.subheadline.weight(.black))
            Spacer()
            Text("\(resultCount) / \(totalCount)作品")
                .font(.subheadline.monospacedDigit().weight(.black))
        }
        .foregroundStyle(AppPalette.ink.opacity(0.72))
        .padding(14)
        .background(AppPalette.gold.opacity(0.16), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct StatusBadge: View {
    let title: String
    let systemImage: String

    var body: some View {
        Label(title, systemImage: systemImage)
            .font(.caption2.weight(.black))
            .foregroundStyle(AppPalette.crimson)
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(AppPalette.crimson.opacity(0.10), in: Capsule())
    }
}

private struct InfoPill: View {
    let title: String
    let value: String

    var body: some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(AppPalette.ink.opacity(0.52))

            Text(value)
                .font(.callout.weight(.black))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .foregroundStyle(AppPalette.ink)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct MangaTonePattern: View {
    var body: some View {
        GeometryReader { proxy in
            Canvas { context, size in
                let spacing: CGFloat = 11
                let radius: CGFloat = 1.5
                var x: CGFloat = 0
                while x < size.width {
                    var y: CGFloat = 0
                    while y < size.height {
                        context.fill(Path(ellipseIn: CGRect(x: x, y: y, width: radius, height: radius)), with: .color(.white))
                        y += spacing
                    }
                    x += spacing
                }

                var path = Path()
                path.move(to: CGPoint(x: 0, y: size.height * 0.18))
                path.addLine(to: CGPoint(x: size.width, y: size.height * 0.02))
                context.stroke(path, with: .color(.white.opacity(0.55)), lineWidth: max(1, proxy.size.width / 260))
            }
        }
    }
}

private struct MangaPick: Identifiable {
    var id: String { title }
    let title: String
    let shortTitle: String
    let genre: String
    let score: String
    let volume: String
    let mood: String
    let pitch: String
    let note: String
    let colors: [Color]

    static let samples: [MangaPick] = [
        MangaPick(title: "夜明け前の編集部", shortTitle: "夜明け前の\n編集部", genre: "ドラマ", score: "9.2", volume: "8巻", mood: "熱量", pitch: "新人編集者と作家が一本の連載に向き合う仕事ドラマ。", note: "締切、才能、街の空気まで丁寧に描く作品。読み終えると何かを始めたくなります。", colors: [AppPalette.ink, AppPalette.crimson]),
        MangaPick(title: "灰色都市のナビゲーター", shortTitle: "灰色都市の\nナビ", genre: "SF", score: "8.9", volume: "12巻", mood: "疾走", pitch: "迷路のような未来都市で、少女が真実への道を引く。", note: "地下鉄、監視網、古い地図。読み進めるほど街の見え方が変わる硬派なSFです。", colors: [Color(red: 0.10, green: 0.12, blue: 0.16), Color(red: 0.72, green: 0.58, blue: 0.22)]),
        MangaPick(title: "ひと駅ぶんの怪談", shortTitle: "ひと駅\n怪談", genre: "ミステリー", score: "8.7", volume: "5巻", mood: "余韻", pitch: "短いのに刺さる、一話完結の駅前ミステリー。", note: "毎話ひと駅で読み切れる構成。静かに回収される伏線が気持ちいい作品です。", colors: [Color(red: 0.16, green: 0.18, blue: 0.20), Color(red: 0.12, green: 0.42, blue: 0.46)]),
        MangaPick(title: "鉄の王女", shortTitle: "鉄の\n王女", genre: "歴史", score: "9.0", volume: "10巻", mood: "重厚", pitch: "勝者の記録に残らなかった人々を描く歴史劇。", note: "派手な合戦より決断の前夜に焦点を置くタイプ。絵の密度も高いです。", colors: [Color(red: 0.30, green: 0.20, blue: 0.14), AppPalette.gold]),
        MangaPick(title: "朝焼けアパートメント", shortTitle: "朝焼け\nアパート", genre: "日常", score: "8.5", volume: "6巻", mood: "静か", pitch: "何も起きない日の奥にある、ちゃんとしたドラマ。", note: "住人それぞれの小さな変化を丁寧に描きます。休日の午前中に合う作品です。", colors: [Color(red: 0.71, green: 0.43, blue: 0.32), Color(red: 0.93, green: 0.78, blue: 0.55)]),
        MangaPick(title: "黒線のラブレター", shortTitle: "黒線の\n手紙", genre: "恋愛", score: "8.8", volume: "7巻", mood: "切ない", pitch: "言えなかった言葉が、一本の線で届いてしまう。", note: "甘さより痛みが残る恋愛もの。表情の描き分けがうまく、ページを戻したくなります。", colors: [Color(red: 0.18, green: 0.13, blue: 0.18), Color(red: 0.67, green: 0.19, blue: 0.28)]),
        MangaPick(title: "屋上の天文部", shortTitle: "屋上の\n天文部", genre: "青春", score: "8.6", volume: "4巻", mood: "爽快", pitch: "星を見るだけだった部活が、学校を少し変えていく。", note: "軽い会話と成長のバランスがよく、まとめて読みやすい青春群像劇です。", colors: [Color(red: 0.08, green: 0.16, blue: 0.28), Color(red: 0.35, green: 0.58, blue: 0.90)]),
        MangaPick(title: "路地裏ベーカリー事件簿", shortTitle: "路地裏\n事件簿", genre: "ミステリー", score: "8.4", volume: "9巻", mood: "軽快", pitch: "パン屋の店主が、近所の小さな謎をほどいていく。", note: "重すぎない謎解きと食べ物描写が魅力。寝る前に一話だけ読みたい時に向きます。", colors: [Color(red: 0.52, green: 0.28, blue: 0.16), Color(red: 0.93, green: 0.72, blue: 0.38)]),
        MangaPick(title: "水晶塔の記録係", shortTitle: "水晶塔の\n記録係", genre: "ファンタジー", score: "9.1", volume: "14巻", mood: "冒険", pitch: "失われた記録を追う旅が、王国の秘密につながる。", note: "世界設定が厚く、旅の目的が少しずつ変わっていく長編ファンタジーです。", colors: [Color(red: 0.12, green: 0.22, blue: 0.34), Color(red: 0.46, green: 0.73, blue: 0.84)]),
        MangaPick(title: "雨音キッチン", shortTitle: "雨音\nキッチン", genre: "日常", score: "8.3", volume: "3巻", mood: "やさしい", pitch: "料理と会話で、登場人物の心が少しほどける。", note: "大きな事件はありません。疲れた日に開くと、ちょうどいい温度で返ってくれます。", colors: [Color(red: 0.23, green: 0.36, blue: 0.34), Color(red: 0.75, green: 0.82, blue: 0.72)]),
        MangaPick(title: "銀河配送便", shortTitle: "銀河\n配送便", genre: "SF", score: "8.6", volume: "11巻", mood: "冒険", pitch: "荷物を届けるだけの仕事が、星々の問題をつなぐ。", note: "一話完結の気持ちよさと大きな物語の引きが両方あります。", colors: [Color(red: 0.08, green: 0.09, blue: 0.20), Color(red: 0.75, green: 0.42, blue: 0.88)]),
        MangaPick(title: "坂道の写真館", shortTitle: "坂道の\n写真館", genre: "ドラマ", score: "8.5", volume: "6巻", mood: "余韻", pitch: "古い写真館に持ち込まれる一枚から人生をたどる。", note: "派手さはないのに忘れにくい短編連作。人物の距離感が丁寧です。", colors: [Color(red: 0.24, green: 0.22, blue: 0.20), Color(red: 0.78, green: 0.60, blue: 0.42)]),
        MangaPick(title: "剣と喫茶の午後", shortTitle: "剣と喫茶の\n午後", genre: "ファンタジー", score: "8.2", volume: "5巻", mood: "軽快", pitch: "勇者をやめた店主の喫茶店に、困りごとが集まる。", note: "バトルより会話が楽しいタイプ。息抜きで読めるファンタジーです。", colors: [Color(red: 0.33, green: 0.23, blue: 0.14), Color(red: 0.81, green: 0.50, blue: 0.27)]),
        MangaPick(title: "透明な裁判", shortTitle: "透明な\n裁判", genre: "サスペンス", score: "8.9", volume: "8巻", mood: "緊張", pitch: "証拠がすべて公開される社会で、嘘はどこに隠れるのか。", note: "会話の圧が強い法廷サスペンス。読者の予想を静かに裏切ります。", colors: [Color(red: 0.09, green: 0.10, blue: 0.11), Color(red: 0.52, green: 0.55, blue: 0.58)]),
        MangaPick(title: "放課後リペア部", shortTitle: "放課後\nリペア部", genre: "青春", score: "8.1", volume: "4巻", mood: "前向き", pitch: "壊れた物を直す部活が、持ち主の記憶まで修理する。", note: "小道具の使い方がうまく、各話の読後感が明るい作品です。", colors: [Color(red: 0.16, green: 0.42, blue: 0.38), Color(red: 0.92, green: 0.68, blue: 0.34)]),
        MangaPick(title: "赤い傘の探偵", shortTitle: "赤い傘の\n探偵", genre: "サスペンス", score: "8.7", volume: "7巻", mood: "緊張", pitch: "雨の日だけ現れる探偵が、消えた人の足取りを追う。", note: "画面の湿度が高く、事件の余韻まで楽しめます。", colors: [Color(red: 0.11, green: 0.12, blue: 0.16), Color(red: 0.72, green: 0.07, blue: 0.10)]),
        MangaPick(title: "書庫街の魔法使い", shortTitle: "書庫街の\n魔法使い", genre: "ファンタジー", score: "8.8", volume: "13巻", mood: "濃密", pitch: "本を読むほど魔法が変わる街で、少年は禁書を探す。", note: "本好きに刺さる設定が多く、巻数が進むほど関係性が深まります。", colors: [Color(red: 0.20, green: 0.12, blue: 0.28), Color(red: 0.78, green: 0.54, blue: 0.24)]),
        MangaPick(title: "潮騒ホームルーム", shortTitle: "潮騒\nホームルーム", genre: "青春", score: "8.4", volume: "6巻", mood: "爽快", pitch: "海辺の学校で、転校生とクラスの一年が始まる。", note: "人間関係の変化をゆっくり追います。夏の空気が好きな人に合います。", colors: [Color(red: 0.09, green: 0.32, blue: 0.50), Color(red: 0.38, green: 0.74, blue: 0.82)])
    ]
}

private enum AppPalette {
    static let ink = Color(red: 0.10, green: 0.10, blue: 0.10)
    static let paper = Color(red: 0.96, green: 0.94, blue: 0.89)
    static let crimson = Color(red: 0.62, green: 0.07, blue: 0.12)
    static let gold = Color(red: 0.84, green: 0.62, blue: 0.25)
}

private extension View {
    func primaryActionStyle() -> some View {
        self
            .font(.headline.weight(.black))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 58)
            .background(AppPalette.crimson, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            .shadow(color: AppPalette.crimson.opacity(0.22), radius: 12, y: 7)
    }

    func secondaryActionStyle(isActive: Bool) -> some View {
        self
            .font(.subheadline.weight(.black))
            .foregroundStyle(isActive ? .white : AppPalette.ink)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(isActive ? AppPalette.crimson : .white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(AppPalette.ink.opacity(0.08), lineWidth: 1)
            )
    }
}
