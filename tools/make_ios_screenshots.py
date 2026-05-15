from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "ios-screenshots"
OUT.mkdir(parents=True, exist_ok=True)

W, H = 1290, 2796
INK = "#191715"
PAPER = "#f5ecd9"
CRIMSON = "#9e121f"
GOLD = "#d6a443"
MUTED = "#6f6356"
LINE = "#e6d8bd"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        r"C:\Windows\Fonts\meiryob.ttc" if bold else r"C:\Windows\Fonts\meiryo.ttc",
        r"C:\Windows\Fonts\YuGothB.ttc" if bold else r"C:\Windows\Fonts\YuGothM.ttc",
        r"C:\Windows\Fonts\msgothic.ttc",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            pass
    return ImageFont.load_default()


F = {
    "hero": font(92, True),
    "xl": font(70, True),
    "lg": font(52, True),
    "md": font(38, True),
    "body": font(32),
    "small": font(25),
    "tiny": font(21, True),
}


def bg() -> Image.Image:
    img = Image.new("RGB", (W, H), PAPER)
    draw = ImageDraw.Draw(img)
    for y in range(0, H, 28):
        draw.line((0, y, W, y), fill="#efe2c9", width=1)
    return img


def rounded(draw: ImageDraw.ImageDraw, box, radius: int, fill: str, outline: str | None = None, width: int = 1) -> None:
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def text(draw: ImageDraw.ImageDraw, xy, value: str, key: str, fill: str = INK, anchor: str | None = None) -> None:
    draw.text(xy, value, font=F[key], fill=fill, anchor=anchor)


def card(draw: ImageDraw.ImageDraw, box, fill: str = "#ffffff") -> None:
    rounded(draw, box, 28, fill, LINE, 3)


def cover(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, title: str, genre: str, colors: tuple[str, str]) -> None:
    for i in range(h):
        r1, g1, b1 = tuple(int(colors[0][j : j + 2], 16) for j in (1, 3, 5))
        r2, g2, b2 = tuple(int(colors[1][j : j + 2], 16) for j in (1, 3, 5))
        mix = i / max(1, h - 1)
        color = (
            int(r1 * (1 - mix) + r2 * mix),
            int(g1 * (1 - mix) + g2 * mix),
            int(b1 * (1 - mix) + b2 * mix),
        )
        draw.line((x, y + i, x + w, y + i), fill=color)
    rounded(draw, (x, y, x + w, y + h), 28, fill=None, outline="#ffffff", width=0)
    rounded(draw, (x + 20, y + 22, x + 148, y + 72), 22, "#ffffff")
    text(draw, (x + 42, y + 34), genre, "tiny", INK)
    text(draw, (x + 28, y + h - 124), title, "md", "#ffffff")


def header(draw: ImageDraw.ImageDraw, title: str, subtitle: str) -> None:
    text(draw, (90, 105), "MangaNavi", "small", CRIMSON)
    text(draw, (90, 168), title, "xl")
    text(draw, (94, 258), subtitle, "body", MUTED)


def home() -> None:
    img = bg()
    draw = ImageDraw.Draw(img)
    rounded(draw, (0, 0, W, 720), 0, INK)
    for x in range(-120, W, 62):
        draw.line((x, 0, x + 460, 720), fill="#3c1919", width=8)
    text(draw, (90, 170), "MangaNavi", "hero", "#ffffff")
    text(draw, (94, 292), "次に読む漫画を、検索と保存で迷わず選ぶ", "body", "#f5ecd9")
    rounded(draw, (90, 380, 520, 465), 28, CRIMSON)
    text(draw, (132, 402), "作品を探す", "md", "#ffffff")
    rounded(draw, (550, 380, 980, 465), 28, "#ffffff")
    text(draw, (592, 402), "読みたい保存", "md", INK)

    y = 790
    for i, (num, label) in enumerate([("54", "作品"), ("9", "ジャンル"), ("2", "リスト")]):
        x = 90 + i * 380
        card(draw, (x, y, x + 340, y + 160))
        text(draw, (x + 35, y + 34), num, "lg")
        text(draw, (x + 35, y + 98), label, "small", MUTED)

    text(draw, (90, 1060), "今日のおすすめ", "lg")
    card(draw, (90, 1145, W - 90, 1515), "#fffaf0")
    cover(draw, 125, 1180, 245, 300, "夜明けの\n編集部", "ドラマ", ("#191715", "#9e121f"))
    text(draw, (405, 1195), "夜明けの編集部", "lg")
    text(draw, (408, 1270), "新人編集者と作家が連載に向き合う仕事ドラマ。", "body", MUTED)
    rounded(draw, (405, 1390, 585, 1455), 22, CRIMSON)
    text(draw, (440, 1404), "詳細", "md", "#ffffff")

    text(draw, (90, 1615), "すぐ使える機能", "lg")
    for i, label in enumerate(["検索", "ジャンル絞り込み", "読みたい保存", "読了管理"]):
        x = 90 + (i % 2) * 560
        y = 1700 + (i // 2) * 180
        card(draw, (x, y, x + 520, y + 130))
        rounded(draw, (x + 28, y + 28, x + 96, y + 96), 20, CRIMSON)
        text(draw, (x + 128, y + 42), label, "md")
    img.save(OUT / "01-home-iphone-6-7.png")


def search() -> None:
    img = bg()
    draw = ImageDraw.Draw(img)
    header(draw, "探す", "作品名、ジャンル、気分で絞り込み")
    card(draw, (90, 380, W - 90, 500))
    text(draw, (130, 415), "作品名・ジャンル・気分で検索", "md", MUTED)

    text(draw, (90, 610), "ジャンルを選ぶ", "lg")
    genres = ["ドラマ", "SF", "ミステリー", "歴史", "日常", "恋愛", "青春", "ファンタジー"]
    for i, genre in enumerate(genres):
        x = 90 + (i % 2) * 560
        y = 700 + (i // 2) * 130
        selected = i == 1
        rounded(draw, (x, y, x + 520, y + 96), 26, CRIMSON if selected else "#ffffff", LINE, 3)
        text(draw, (x + 45, y + 25), ("✓ " if selected else "○ ") + genre, "md", "#ffffff" if selected else INK)

    text(draw, (90, 1280), "SFの6作品を表示中", "body", MUTED)
    rows = [("灰色都市のナビゲーター", "8.9", "疾走"), ("銀河配送便", "8.6", "冒険")]
    for i, (title, score, mood) in enumerate(rows):
        y = 1370 + i * 360
        card(draw, (90, y, W - 90, y + 280))
        cover(draw, 125, y + 25, 185, 230, title[:4] + "\n" + title[4:8], "SF", ("#10131a", "#b89438"))
        text(draw, (345, y + 45), title, "lg")
        text(draw, (350, y + 122), f"評価 {score}  気分 {mood}", "body", MUTED)
        rounded(draw, (350, y + 190, 570, y + 250), 20, CRIMSON)
        text(draw, (382, y + 202), "読みたい", "md", "#ffffff")
    img.save(OUT / "02-ranking-iphone-6-7.png")


def detail() -> None:
    img = bg()
    draw = ImageDraw.Draw(img)
    header(draw, "作品詳細", "保存と読了を一画面で管理")
    cover(draw, 390, 390, 510, 690, "灰色都市の\nナビゲーター", "SF", ("#10131a", "#b89438"))
    text(draw, (90, 1190), "灰色都市のナビゲーター", "xl")
    text(draw, (94, 1282), "迷路のような未来都市で、少女が真実への道を引く。", "body", MUTED)
    for i, (label, value) in enumerate([("評価", "8.9"), ("巻数", "12巻"), ("気分", "疾走")]):
        x = 90 + i * 380
        card(draw, (x, 1435, x + 340, 1585))
        text(draw, (x + 35, 1470), label, "small", MUTED)
        text(draw, (x + 35, 1515), value, "md")
    rounded(draw, (90, 1720, 610, 1830), 28, CRIMSON)
    text(draw, (200, 1750), "読みたい", "lg", "#ffffff")
    rounded(draw, (680, 1720, W - 90, 1830), 28, "#ffffff", LINE, 3)
    text(draw, (770, 1750), "読了にする", "lg", INK)
    card(draw, (90, 1950, W - 90, 2240), "#fffaf0")
    text(draw, (130, 1990), "作品メモ", "lg")
    text(draw, (132, 2068), "地下鉄、監視網、古い地図。読み進めるほど街の見え方が変わる硬派なSFです。", "body", MUTED)
    img.save(OUT / "03-detail-iphone-6-7.png")


def list_screen() -> None:
    img = bg()
    draw = ImageDraw.Draw(img)
    header(draw, "読書リスト", "保存した作品と読了作品を整理")
    sections = [
        ("保存した作品", "夜明けの編集部", "ドラマ"),
        ("読了した作品", "黒線のラブレター", "恋愛"),
        ("次に読む候補", "水晶塔の記録係", "ファンタジー"),
    ]
    y = 430
    for section, title, genre in sections:
        text(draw, (90, y), section, "lg")
        card(draw, (90, y + 90, W - 90, y + 330))
        cover(draw, 125, y + 115, 160, 190, title[:4] + "\n" + title[4:8], genre, ("#191715", "#9e121f"))
        text(draw, (325, y + 130), title, "lg")
        text(draw, (328, y + 205), f"{genre}  評価 8.8", "body", MUTED)
        y += 520
    rounded(draw, (90, 2150, W - 90, 2260), 28, CRIMSON)
    text(draw, (260, 2180), "探すタブで作品を追加", "lg", "#ffffff")
    img.save(OUT / "04-list-iphone-6-7.png")


home()
search()
detail()
list_screen()
print(OUT)
