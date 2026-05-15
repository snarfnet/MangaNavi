import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var adService: AdService
    @AppStorage("didCompleteTrackingIntro") private var didCompleteTrackingIntro = false

    var body: some View {
        if didCompleteTrackingIntro {
            MangaNaviHomeView()
                .task {
                    await adService.start()
                }
        } else {
            TrackingIntroView {
                Task {
                    await adService.start()
                    didCompleteTrackingIntro = true
                }
            }
        }
    }
}

private struct TrackingIntroView: View {
    let action: () -> Void

    var body: some View {
        ZStack {
            AppPalette.paper.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 20) {
                Spacer()

                Image(systemName: "rectangle.stack.fill")
                    .font(.system(size: 54, weight: .black))
                    .foregroundStyle(AppPalette.crimson)

                Text("MangaNavi")
                    .font(.system(size: 42, weight: .black, design: .serif))
                    .foregroundStyle(AppPalette.ink)

                Text("広告を表示しながら無料で使えます。次の画面で、広告の表示と効果測定に使うトラッキング許可を確認します。許可しなくても、検索・ランキング・読書リストはそのまま使えます。")
                    .font(.body.weight(.semibold))
                    .lineSpacing(5)
                    .foregroundStyle(AppPalette.ink.opacity(0.76))

                Spacer()

                Button(action: action) {
                    Label("続ける", systemImage: "arrow.right")
                        .primaryActionStyle()
                }
            }
            .padding(24)
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
                NavigationStack {
                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 24) {
                            HeroPanel(featured: dailyPick) {
                                selectedManga = dailyPick
                            }

                            HStack(spacing: 10) {
                                MiniMetric(title: "保存", value: "\(savedSet.count)")
                                MiniMetric(title: "読了", value: "\(finishedSet.count)")
                                MiniMetric(title: "候補", value: "\(filteredPicks.count)")
                            }

                            TextField("作品名・ジャンル・気分で検索", text: $searchText)
                                .textInputAutocapitalization(.never)
                                .disableAutocorrection(true)
                                .padding(14)
                                .background(.white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .stroke(AppPalette.ink.opacity(0.08), lineWidth: 1)
                                )

                            SectionHeader(title: "ジャンルから探す", action: selectedGenre == nil ? nil : "解除")
                                .onTapGesture {
                                    selectedGenre = nil
                                }

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 10) {
                                    ForEach(genres, id: \.self) { genre in
                                        Button {
                                            selectedGenre = selectedGenre == genre ? nil : genre
                                        } label: {
                                            GenreChip(title: genre, isSelected: selectedGenre == genre)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .padding(.vertical, 2)
                            }

                            SectionHeader(title: "おすすめ作品", action: "気分で選ぶ")
                                .onTapGesture {
                                    selectedManga = filteredPicks.randomElement() ?? dailyPick
                                }

                            LazyVGrid(columns: columns, spacing: 14) {
                                ForEach(filteredPicks) { manga in
                                    MangaCard(
                                        manga: manga,
                                        isSaved: savedSet.contains(manga.title),
                                        isFinished: finishedSet.contains(manga.title)
                                    )
                                    .onTapGesture {
                                        selectedManga = manga
                                    }
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
                .tabItem {
                    Label("ホーム", systemImage: "book.pages")
                }
                .tag(0)

                RankingView(picks: picks, selectedManga: $selectedManga)
                    .tabItem {
                        Label("ランキング", systemImage: "chart.bar.fill")
                    }
                    .tag(1)

                ReadingListView(
                    picks: picks,
                    selectedManga: $selectedManga,
                    savedTitles: savedSet,
                    finishedTitles: finishedSet
                )
                    .tabItem {
                        Label("リスト", systemImage: "bookmark.fill")
                    }
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

    private var filteredPicks: [MangaPick] {
        picks.filter { manga in
            let matchesGenre = selectedGenre == nil || manga.genre == selectedGenre
            let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
            let matchesSearch = query.isEmpty ||
                manga.title.localizedCaseInsensitiveContains(query) ||
                manga.genre.localizedCaseInsensitiveContains(query) ||
                manga.mood.localizedCaseInsensitiveContains(query) ||
                manga.pitch.localizedCaseInsensitiveContains(query)
            return matchesGenre && matchesSearch
        }
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

                    Spacer(minLength: 40)

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

private struct RankingView: View {
    let picks: [MangaPick]
    @Binding var selectedManga: MangaPick?

    var body: some View {
        NavigationStack {
            List(Array(picks.enumerated()), id: \.element.id) { index, manga in
                Button {
                    selectedManga = manga
                } label: {
                    HStack(spacing: 14) {
                        Text("\(index + 1)")
                            .font(.title2.monospacedDigit().weight(.black))
                            .frame(width: 42, height: 42)
                            .background(index == 0 ? AppPalette.gold : AppPalette.ink.opacity(0.08))
                            .foregroundStyle(index == 0 ? AppPalette.ink : AppPalette.ink.opacity(0.72))
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                        VStack(alignment: .leading, spacing: 5) {
                            Text(manga.title)
                                .font(.headline)
                                .foregroundStyle(AppPalette.ink)

                            Text(manga.genre)
                                .font(.caption.weight(.bold))
                                .foregroundStyle(AppPalette.crimson)
                        }

                        Spacer()

                        Text(manga.score)
                            .font(.callout.monospacedDigit().weight(.bold))
                            .foregroundStyle(AppPalette.ink)
                    }
                    .padding(.vertical, 8)
                }
            }
            .scrollContentBackground(.hidden)
            .background(AppPalette.paper)
            .navigationTitle("ランキング")
        }
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
                    ReadingGroup(title: "保存した作品", emptyText: "作品詳細から保存するとここに並びます。") {
                        listRows(for: picks.filter { savedTitles.contains($0.title) })
                    }

                    ReadingGroup(title: "読了した作品", emptyText: "読み終えた作品を記録できます。") {
                        listRows(for: picks.filter { finishedTitles.contains($0.title) })
                    }

                    ReadingGroup(title: "次に読む候補", emptyText: nil) {
                        listRows(for: picks.filter { !finishedTitles.contains($0.title) }.prefix(4).map { $0 })
                    }
                }
                .padding(20)
            }
            .background(AppPalette.paper.ignoresSafeArea())
            .navigationTitle("読書リスト")
        }
    }

    private func listRows(for rows: [MangaPick]) -> some View {
        VStack(spacing: 12) {
            if rows.isEmpty {
                EmptyListText()
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
                    .font(.system(size: 24, weight: .black, design: .serif))
                    .lineLimit(3)
                    .minimumScaleFactor(0.68)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .foregroundStyle(.white)
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
                VStack(alignment: .leading, spacing: 22) {
                    CoverMark(manga: manga)
                        .frame(height: 360)

                    VStack(alignment: .leading, spacing: 10) {
                        Text(manga.title)
                            .font(.system(size: 34, weight: .black, design: .serif))
                            .foregroundStyle(AppPalette.ink)

                        Text(manga.pitch)
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(AppPalette.crimson)

                        Text(manga.note)
                            .font(.body)
                            .lineSpacing(5)
                            .foregroundStyle(AppPalette.ink.opacity(0.72))
                    }

                    HStack(spacing: 10) {
                        InfoPill(title: "評価", value: manga.score)
                        InfoPill(title: "巻数", value: manga.volume)
                        InfoPill(title: "気分", value: manga.mood)
                    }

                    HStack(spacing: 10) {
                        Button(action: toggleSaved) {
                            Label(isSaved ? "保存を外す" : "読みたい", systemImage: isSaved ? "bookmark.slash.fill" : "bookmark.fill")
                                .secondaryActionStyle(isActive: isSaved)
                        }

                        Button(action: toggleFinished) {
                            Label(isFinished ? "読了を外す" : "読了にする", systemImage: isFinished ? "arrow.uturn.backward.circle.fill" : "checkmark.seal.fill")
                                .secondaryActionStyle(isActive: isFinished)
                        }
                    }
                }
                .padding(20)
            }
            .background(AppPalette.paper.ignoresSafeArea())
            .navigationTitle("作品詳細")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("閉じる") {
                        dismiss()
                    }
                }
            }
        }
    }
}

private struct ReadingGroup<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, emptyText: String? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: title, action: nil)
            content
        }
    }
}

private struct EmptyListText: View {
    var body: some View {
        Text("まだありません")
            .font(.subheadline.weight(.bold))
            .foregroundStyle(AppPalette.ink.opacity(0.52))
            .frame(maxWidth: .infinity, minHeight: 54)
            .background(.white.opacity(0.64), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
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

private struct GenreChip: View {
    let title: String
    var isSelected = false

    var body: some View {
        Label(title, systemImage: "diamond.fill")
            .font(.callout.weight(.bold))
            .foregroundStyle(isSelected ? .white : AppPalette.ink)
            .padding(.horizontal, 14)
            .frame(minHeight: 46)
            .background(isSelected ? AppPalette.crimson : .white, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
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
                        let rect = CGRect(x: x, y: y, width: radius, height: radius)
                        context.fill(Path(ellipseIn: rect), with: .color(.white))
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
        MangaPick(
            title: "夜明けの編集部",
            shortTitle: "夜明けの\n編集部",
            genre: "ドラマ",
            score: "9.2",
            volume: "8巻",
            mood: "熱量",
            pitch: "仕事、才能、締切。静かな火花が走る群像劇。",
            note: "新人編集者と崖っぷち作家が、一本の連載をめぐって街の空気まで変えていく。",
            colors: [AppPalette.ink, AppPalette.crimson]
        ),
        MangaPick(
            title: "灰色都市のナビゲーター",
            shortTitle: "灰色都市の\nナビ",
            genre: "SF",
            score: "8.9",
            volume: "12巻",
            mood: "疾走",
            pitch: "迷路みたいな未来都市で、少女が真実への道を引く。",
            note: "地下鉄、監視網、古い地図。読後に街の見え方が少し変わる硬派なSF。",
            colors: [Color(red: 0.10, green: 0.12, blue: 0.16), Color(red: 0.72, green: 0.58, blue: 0.22)]
        ),
        MangaPick(
            title: "ひと駅ぶんの怪談",
            shortTitle: "ひと駅\n怪談",
            genre: "ミステリー",
            score: "8.7",
            volume: "5巻",
            mood: "余韻",
            pitch: "短いのに刺さる。電車で読む一話完結ミステリー。",
            note: "毎話ひと駅で読み切れる構成。伏線が静かに回収される気持ちよさがある。",
            colors: [Color(red: 0.16, green: 0.18, blue: 0.20), Color(red: 0.12, green: 0.42, blue: 0.46)]
        ),
        MangaPick(
            title: "錆びた王冠",
            shortTitle: "錆びた\n王冠",
            genre: "歴史",
            score: "9.0",
            volume: "10巻",
            mood: "重厚",
            pitch: "勝者の記録に残らなかった人々を描く歴史劇。",
            note: "派手な合戦より、決断の前夜に焦点を置くタイプ。絵の密度も高い。",
            colors: [Color(red: 0.30, green: 0.20, blue: 0.14), AppPalette.gold]
        ),
        MangaPick(
            title: "朝焼けアパートメント",
            shortTitle: "朝焼け\nアパート",
            genre: "日常",
            score: "8.5",
            volume: "6巻",
            mood: "静か",
            pitch: "何も起きない日の奥にある、ちゃんとしたドラマ。",
            note: "住人それぞれの小さな変化を丁寧に描く。休みの日の午前中に合う作品。",
            colors: [Color(red: 0.71, green: 0.43, blue: 0.32), Color(red: 0.93, green: 0.78, blue: 0.55)]
        ),
        MangaPick(
            title: "黒線のラブレター",
            shortTitle: "黒線の\n手紙",
            genre: "恋愛",
            score: "8.8",
            volume: "7巻",
            mood: "切実",
            pitch: "言えなかった言葉が、一本の線で届いてしまう。",
            note: "甘さより痛みが残る恋愛もの。表情の描き分けがうまく、ページを戻したくなる。",
            colors: [Color(red: 0.18, green: 0.13, blue: 0.18), Color(red: 0.67, green: 0.19, blue: 0.28)]
        )
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
