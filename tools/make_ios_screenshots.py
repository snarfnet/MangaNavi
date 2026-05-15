from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "ios-screenshots"
OUT.mkdir(parents=True, exist_ok=True)

HERO = Image.open(ROOT / "composeApp/src/commonMain/composeResources/drawable/manga_hero.png").convert("RGB")
W, H = 1290, 2796


def font(size, bold=False):
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
    "xxl": font(94, True),
    "xl": font(72, True),
    "lg": font(52, True),
    "md": font(38, True),
    "body": font(32),
    "small": font(25),
    "tiny": font(21, True),
}


def cover_crop(img, size):
    w, h = img.size
    tw, th = size
    scale = max(tw / w, th / h)
    nw, nh = int(w * scale), int(h * scale)
    resized = img.resize((nw, nh), Image.Resampling.LANCZOS)
    return resized.crop(((nw - tw) // 2, (nh - th) // 2, (nw + tw) // 2, (nh + th) // 2))


def rounded(draw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def paste_round(base, img, xy, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, img.size[0], img.size[1]), radius=radius, fill=255)
    base.paste(img, xy, mask)


def write(draw, xy, value, fnt, fill, anchor=None):
    draw.text(xy, value, font=fnt, fill=fill, anchor=anchor)


def base_bg():
    img = Image.new("RGB", (W, H), "#fff4dc")
    draw = ImageDraw.Draw(img)
    for y in range(0, H, 24):
        draw.line((0, y, W, y), fill="#f1e5cb", width=1)
    return img


def phone_shell():
    img = base_bg()
    draw = ImageDraw.Draw(img)
    rounded(draw, (0, 0, W, H), 0, "#fff4dc")
    return img


def status(draw, dark=False):
    return


def card(draw, box, fill="#ffffff", outline="#e6d8bd", radius=34):
    rounded(draw, box, radius, fill, outline, 3)


def cover_tile(img, x, y, rank, title, score, color="#e6362e"):
    draw = ImageDraw.Draw(img)
    card(draw, (x, y, x + 350, y + 560), "#ffffff", "#e7d7bc", 28)
    paste_round(img, cover_crop(HERO, (350, 390)), (x, y), 28)
    rounded(draw, (x + 18, y + 18, x + 118, y + 66), 20, "#111015")
    write(draw, (x + 35, y + 25), f"#{rank}", F["tiny"], "#ffc857")
    write(draw, (x + 24, y + 414), title, F["body"], "#111015")
    rounded(draw, (x + 24, y + 500, x + 128, y + 538), 18, color)
    write(draw, (x + 42, y + 505), f"★ {score}", F["tiny"], "#ffffff")


def home():
    img = phone_shell()
    draw = ImageDraw.Draw(img)
    paste_round(img, cover_crop(HERO, (W, 1030)), (0, 0), 0)
    overlay = Image.new("RGBA", (W, 1030), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    for i in range(1030):
        od.line((0, i, W - 184, i), fill=(0, 0, 0, int(20 + 190 * (i / 1030))))
    img.paste(overlay, (0, 0), overlay)
    status(draw, True)
    write(draw, (130, 850), "MangaNavi", F["xxl"], "#ffffff")
    write(draw, (134, 965), "読むべき一冊を、ランキングで一瞬で見つける。", F["body"], "#fff4dc")
    rounded(draw, (134, 1050, 560, 1135), 28, "#e6362e")
    write(draw, (185, 1070), "ランキングを見る", F["md"], "#ffffff")
    actions = [
        ("#1", "総合ランキング", "スコア・人気・勢い", "#e6362e"),
        ("◆", "隠れた名作", "穴場を掘る", "#7357ff"),
        ("＋", "マイリスト", "読みたいを管理", "#38d8ff"),
        ("★", "受賞作", "信頼の名作", "#ffc857"),
    ]
    for i, (mark, title, subtitle, color) in enumerate(actions):
        x = 130 + (i % 2) * 520
        y = 1220 + (i // 2) * 180
        card(draw, (x, y, x + 480, y + 145), "#ffffff", "#e6d8bd", 30)
        rounded(draw, (x + 26, y + 30, x + 104, y + 108), 20, color)
        write(draw, (x + 47, y + 48), mark, F["small"], "#111015")
        write(draw, (x + 130, y + 34), title, F["md"], "#111015")
        write(draw, (x + 132, y + 88), subtitle, F["small"], "#6f6356")
    write(draw, (130, 1665), "今動いている漫画", F["lg"], "#111015")
    cover_tile(img, 130, 1750, 1, "話題作", 92)
    cover_tile(img, 500, 1750, 2, "殿堂入り", 89, "#111015")
    cover_tile(img, 870, 1750, 3, "新鋭", 86, "#7357ff")
    img.save(OUT / "01-home-iphone-6-7.png")


def ranking():
    img = phone_shell()
    draw = ImageDraw.Draw(img)
    rounded(draw, (0, 0, W, 560), 0, "#111015")
    for x in range(0, W, 28):
        draw.line((x, 0, x + 300, 560), fill="#431a1a", width=5)
    status(draw, True)
    write(draw, (130, 245), "MANGA RANKING", F["small"], "#ffc857")
    write(draw, (130, 310), "読者評価が強い作品", F["xl"], "#ffffff")
    for i, label in enumerate(["高評価", "人気", "急上昇"]):
        x = 130 + i * 340
        fill = "#ffc857" if i == 0 else "#ffffff"
        rounded(draw, (x, 610, x + 300, 680), 24, fill, "#e6d8bd")
        write(draw, (x + 150, 628), label, F["md"], "#111015", anchor="ma")
    card(draw, (130, 730, W - 130, 880), "#111015", "#111015", 34)
    write(draw, (165, 768), "現在の1位", F["small"], "#ffc857")
    write(draw, (165, 812), "葬送のフリーレン", F["md"], "#fff4dc")
    rounded(draw, (960, 780, 1130, 850), 22, "#e6362e")
    write(draw, (1002, 796), "★ 92", F["md"], "#ffffff")
    titles = ["葬送のフリーレン", "チ。-地球の運動について-", "ゴールデンカムイ", "ブルーピリオド", "ダイヤモンドの功罪"]
    for i, title in enumerate(titles):
        y = 930 + i * 300
        card(draw, (130, y, W - 130, y + 250), "#ffffff", "#e6d8bd", 34)
        rounded(draw, (160, y + 35, 235, y + 215), 24, "#ffc857" if i < 3 else "#111015")
        write(draw, (180, y + 82), f"#{i + 1}", F["md"], "#111015" if i < 3 else "#ffc857")
        paste_round(img, cover_crop(HERO, (145, 205)), (260, y + 24), 20)
        write(draw, (435, y + 40), title, F["md"], "#111015")
        write(draw, (438, y + 98), "連載中  ・  JP Manga", F["small"], "#6f6356")
        rounded(draw, (438, y + 155, 548, y + 200), 18, "#111015")
        write(draw, (462, y + 163), f"★ {92 - i * 2}", F["tiny"], "#ffc857")
    img.save(OUT / "02-ranking-iphone-6-7.png")


def detail():
    img = phone_shell()
    draw = ImageDraw.Draw(img)
    paste_round(img, cover_crop(HERO, (W, 620)), (0, 0), 0)
    overlay = Image.new("RGBA", (W, 620), (0, 0, 0, 80))
    img.paste(overlay, (0, 0), overlay)
    status(draw, True)
    rounded(draw, (130, 210, 250, 265), 24, (255, 255, 255, 60))
    write(draw, (158, 222), "戻る", F["small"], "#ffffff")
    paste_round(img, cover_crop(HERO, (360, 510)), (465, 360), 30)
    write(draw, (130, 800), "作品詳細", F["small"], "#6f6356")
    write(draw, (130, 850), "読む前に知りたい情報を一画面に集約。", F["lg"], "#111015")
    card(draw, (130, 1030, W - 130, 1200), "#fffaf0", "#e6d8bd", 34)
    write(draw, (180, 1072), "★ 92%", F["xl"], "#111015")
    write(draw, (390, 1088), "神作クラスの高評価", F["md"], "#e6362e")
    labels = [("巻", "巻数", "12巻"), ("話", "話数", "128話"), ("人", "人気度", "540K")]
    for i, (mark, label, value) in enumerate(labels):
        x = 170 + i * 330
        card(draw, (x, 1280, x + 290, 1445), "#ffffff", "#e6d8bd", 28)
        write(draw, (x + 35, 1315), mark, F["md"], "#111015")
        write(draw, (x + 35, 1365), value, F["md"], "#111015")
        write(draw, (x + 38, 1410), label, F["small"], "#6f6356")
    write(draw, (130, 1540), "どこで読める？", F["lg"], "#111015")
    for i, site in enumerate(["Kindle", "BOOK WALKER", "ebookjapan"]):
        y = 1630 + i * 120
        card(draw, (130, y, W - 130, y + 92), "#ffffff", "#e6d8bd", 24)
        write(draw, (170, y + 26), "↗", F["md"], "#e6362e")
        write(draw, (240, y + 28), site, F["md"], "#111015")
    img.save(OUT / "03-detail-iphone-6-7.png")


def reading_list():
    img = phone_shell()
    draw = ImageDraw.Draw(img)
    status(draw, False)
    write(draw, (126, 220), "マイリスト", F["lg"], "#111015")
    write(draw, (128, 284), "読みたい・読書中・読了を管理", F["small"], "#6f6356")
    for i, tab in enumerate(["読みたい", "読書中", "読了"]):
        x = 130 + i * 340
        rounded(draw, (x, 360, x + 300, 430), 24, "#111015" if i == 0 else "#ffffff", "#e6d8bd")
        write(draw, (x + 150, 378), tab, F["md"], "#ffc857" if i == 0 else "#111015", anchor="ma")
    titles = ["ダイヤモンドの功罪", "光が死んだ夏", "ルックバック", "海が走るエンドロール", "ブルーピリオド"]
    for i, title in enumerate(titles):
        y = 510 + i * 250
        card(draw, (130, y, W - 130, y + 200), "#ffffff", "#e6d8bd", 30)
        paste_round(img, cover_crop(HERO, (120, 170)), (160, y + 15), 20)
        write(draw, (310, y + 35), title, F["md"], "#111015")
        write(draw, (312, y + 95), "★ 88  ・  読みたい", F["small"], "#6f6356")
        rounded(draw, (312, y + 138, 470, y + 178), 18, "#e6362e")
        write(draw, (338, y + 145), "読書中へ", F["tiny"], "#ffffff")
    img.save(OUT / "04-list-iphone-6-7.png")


home()
ranking()
detail()
reading_list()
print(OUT)
