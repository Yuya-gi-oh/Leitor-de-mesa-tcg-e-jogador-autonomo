import math
import random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1280, 720
FPS = 12
SLIDE_SECONDS = 6.5
FADE_FRAMES = 7
FONT_REG = "/tmp/fonts/dejavu-fonts-ttf-2.37/ttf/DejaVuSans.ttf"
FONT_BOLD = "/tmp/fonts/dejavu-fonts-ttf-2.37/ttf/DejaVuSans-Bold.ttf"

def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)

BG = (13, 17, 23)
PANEL = (22, 27, 34)
GREEN = (46, 125, 50)
GREEN_L = (129, 199, 132)
TEAL = (0, 105, 92)
PURPLE = (106, 27, 154)
GREY = (139, 148, 158)
WHITE = (235, 240, 245)
AMBER = (255, 179, 0)

def wrap(text, f, maxw, d):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if d.textlength(t, font=f) <= maxw:
            cur = t
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines

def rounded(img_or_d, box, r, fill, outline=None, width=1):
    if hasattr(img_or_d, 'rounded_rectangle'):
        img_or_d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)
    else:
        ImageDraw.Draw(img_or_d).rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)

def phone(target, painter):
    px, py, pw, ph = 800, 50, 360, 620
    rounded(target, (px - 6, py - 6, px + pw + 6, py + ph + 6), 36, (60, 65, 75))
    screen = Image.new("RGB", (pw, ph), (18, 22, 28))
    pd = ImageDraw.Draw(screen)
    painter(pd, pw, ph)
    mask = Image.new("L", (pw, ph), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, pw - 1, ph - 1), 30, fill=255)
    target.paste(screen, (px, py), mask)
    dr = ImageDraw.Draw(target)
    dr.rounded_rectangle((px - 1000, py - 1000, px + pw + 1000, py + ph + 1000), 36, outline=None)

def chip(d, x, y, text, f, selected, pad=14):
    tw = d.textlength(text, font=f)
    h = f.size + 18
    box = (x, y, x + tw + pad * 2, y + h)
    if selected:
        rounded(d, box, h // 2, GREEN)
    else:
        rounded(d, box, h // 2, PANEL, outline=GREY, width=1)
    d.text((x + pad, y + 8), text, font=f, fill=WHITE)
    return d.textlength(text, font=f) + pad * 2

def card_rect(d, x, y, w, h, face_up=True, tint=(45, 55, 70), label="", f=None):
    rounded(d, (x, y, x + w, y + h), 8, tint, outline=GREY, width=2)
    if face_up and label:
        d.text((x + 6, y + 4), label, font=f, fill=WHITE)



def mock_home(d, w, h, hi_game=-1, show_format=False, hi_format=-1, hi_diff=-1):
    d.text((20, 12), "Escolhe o jogo", font=font(16, True), fill=GREY)
    x = 20
    for i, g in enumerate(["Magic: The Gathering", "Yu-Gi-Oh!"]):
        x += chip(d, x, 42, g, font(15), i == hi_game) + 10
    if show_format:
        d.text((20, 100), "Formato de duelo", font=font(16, True), fill=GREY)
        x = 20
        for i, g in enumerate(["Padrão", "Speed Duel"]):
            x += chip(d, x, 130, g, font(15), i == hi_format) + 10
    d.text((20, 186), "Escolhe a dificuldade da IA", font=font(16, True), fill=GREY)
    y = 216
    for i, (name, desc) in enumerate([("Fácil", "aleatório legal"), ("Médio", "heurística"),
                                      ("Difícil", "minimax"), ("Mestre", "MCTS")]):
        sel = i == hi_diff
        rounded(d, (20, y, 180, y + 30), 15, GREEN if sel else PANEL, outline=GREY, width=1)
        d.text((34, y + 6), name, font=font(15), fill=WHITE)
        d.text((190, y + 8), desc, font=font(13), fill=GREY)
        y += 40
    rounded(d, (20, h - 60, w - 20, h - 20), 10, GREEN)
    d.text((60, h - 50), "Iniciar partida", font=font(17, True), fill=WHITE)

def mock_setup(d, w, h):
    d.text((16, 10), "Quais são as tuas cartas?", font=font(17, True), fill=WHITE)
    rows = [("Grizzly Bears", "x14"), ("Hill Giant", "x8"), ("Shock", "x4"), ("Plains", "x10")]
    y = 52
    for name, qty in rows:
        rounded(d, (16, y, w - 16, y + 64), 10, PANEL, outline=GREY, width=1)
        d.text((28, y + 10), name, font=font(15), fill=WHITE)
        d.text((28, y + 34), "Creature · 2 mana", font=font(11), fill=GREY)
        rounded(d, (w - 108, y + 16, w - 76, y + 48), 6, (35, 42, 52))
        d.text((w - 100, y + 22), "-", font=font(16, True), fill=WHITE)
        d.text((w - 66, y + 22), qty, font=font(14, True), fill=AMBER)
        rounded(d, (w - 46, y + 16, w - 14, y + 48), 6, (35, 42, 52))
        d.text((w - 34, y + 22), "+", font=font(16, True), fill=WHITE)
        y += 76
    d.text((16, y + 2), "Baralho: 36 cartas (mínimo 40)", font=font(14, True), fill=AMBER)
    rounded(d, (16, h - 96, w - 16, h - 60), 10, (35, 42, 52), outline=GREY, width=1)
    d.text((40, h - 88), "Usar baralho de exemplo", font=font(14), fill=WHITE)
    rounded(d, (16, h - 52, w - 16, h - 14), 10, GREEN)
    d.text((52, h - 44), "Começar duelo", font=font(16, True), fill=WHITE)

def mock_game(d, w, h):
    rounded(d, (16, 10, w - 16, 86), 10, (40, 22, 26), outline=GREY)
    d.text((26, 18), "Adversário (IA) — Difícil", font=font(13, True), fill=AMBER)
    d.text((26, 44), "Vida: 17   Baralho: 48   Mão: 4", font=font(13), fill=WHITE)
    for i in range(3):
        card_rect(d, 16 + i * 74, 96, 62, 88, face_up=(i != 2),
                  label=["Celtic", "Skull", ""][i], f=font(11))
    rounded(d, (16, 194, w - 16, 300), 10, (20, 38, 26), outline=GREY)
    d.text((26, 202), "O teu tabuleiro", font=font(13, True), fill=GREEN_L)
    d.text((26, 228), "Vida: 20   Mana: 3   Mão: 5", font=font(13), fill=WHITE)
    for i in range(3):
        card_rect(d, 16 + i * 74, 234 if False else 252, 62, 0 if False else 40) 
    d.text((16, 308), "Jogadas legais (toque para executar)", font=font(13, True), fill=GREY)
    y = 332
    for a in ["Invocar Shivan Dragon", "Jogar Lightning Bolt", "Entrar na fase de batalha",
              "Passar para a fase seguinte"]:
        rounded(d, (16, y, w - 16, y + 36), 8, PANEL, outline=GREEN, width=1)
        d.text((26, y + 9), a, font=font(13), fill=WHITE)
        y += 44
    d.text((16, y + 2), "Explicações da IA", font=font(13, True), fill=GREY)
    y += 24
    for msg in ["IA: Shivan Dragon ataca Hill Giant", "Tu: Jogar Lightning Bolt"]:
        rounded(d, (16, y, w - 16, y + 30), 8, (26, 32, 40))
        d.text((24, y + 7), msg, font=font(12), fill=GREY)
        y += 38

def mock_camera(d, w, h):
    for i in range(60):
        c = 8 + (i * 5) % 30
        d.rectangle((0, i * 11, w, (i + 1) * 11), fill=(c, c + 6, c + 10))
    cx, cy, cw, ch = w // 2 - 90, 120, 180, 260
    for (a, b) in [((cx - 14, cy - 14), (cx + 26, cy - 14)), ((cx - 14, cy - 14), (cx - 14, cy + 26)),
                   ((cx + cw - 26, cy - 14), (cx + cw + 14, cy - 14)), ((cx + cw + 14, cy - 14), (cx + cw + 14, cy + 26)),
                   ((cx - 14, cy + ch - 26), (cx - 14, cy + ch + 14)), ((cx - 14, cy + ch + 14), (cx + 26, cy + ch + 14)),
                   ((cx + cw + 14, cy + ch - 26), (cx + cw + 14, cy + ch + 14)), ((cx + cw - 26, cy + ch + 14), (cx + cw + 14, cy + ch + 14))]:
        d.line([a, b], fill=GREEN_L, width=4)
    card_rect(d, cx, cy, cw, ch, face_up=True, tint=(62, 66, 74))
    d.text((cx + 12, cy + 110), "Grizzly Bears", font=font(12), fill=WHITE)
    d.text((cx + 12, cy + 132), "Creature", font=font(10), fill=GREY)
    rounded(d, (30, h - 150, w - 30, h - 110), 8, GREEN)
    d.text((48, h - 142), "Reconhecida: Grizzly Bears  ✓  adicionar ao campo", font=font(13), fill=WHITE)
    d.text((w // 2 - 70, h - 92), "A ler a mesa…", font=font(14, True), fill=WHITE)

def mock_end(d, w, h):
    rounded(d, (16, 30, w - 16, 120), 12, (24, 44, 28), outline=GREEN_L, width=2)
    d.text((24, 44), "Tu — Vida: 20", font=font(16, True), fill=GREEN_L)
    d.text((24, 76), "IA — Vida: 0", font=font(16), fill=GREY)
    d.text((24, 140), "Ganhaste a partida!", font=font(20, True), fill=WHITE)
    y = 200
    for msg in ["IA: (baralho vazio)", "O baralho esgotou — fim do jogo"]:
        rounded(d, (16, y, w - 16, y + 32), 8, PANEL)
        d.text((24, y + 8), msg, font=font(12), fill=GREY)
        y += 40
    rounded(d, (16, h - 60, w - 16, h - 20), 10, GREEN)
    d.text((64, h - 50), "Nova partida", font=font(17, True), fill=WHITE)

def base_slide(bg_img):
    img = bg_img.copy()
    d = ImageDraw.Draw(img)
    return img, d

def left_block(slide, step, title, bullets):
    d = ImageDraw.Draw(slide)
    fstep = font(22, True)
    d.text((70, 60), step, font=fstep, fill=GREEN_L)
    ftitle = font(40, True)
    y = 100
    for line in wrap(title, ftitle, 620, d):
        d.text((70, y), line, font=ftitle, fill=WHITE)
        y += 52
    y += 14
    fb = font(22)
    for b in bullets:
        d.ellipse((76, y + 10, 88, y + 22), fill=GREEN)
        lines = wrap(b, fb, 590, d)
        for ln in lines:
            d.text((102, y), ln, font=fb, fill=(200, 208, 216))
            y += 32
        y += 10
    return slide

def title_slide():
    img = Image.new("RGB", (W, H), BG)
    rnd = random.Random(7)
    overlay = Image.new("RGB", (W, H), BG)
    od = ImageDraw.Draw(overlay)
    for i in range(5):
        x, y = 90 + i * 240, 300 + ((i * 37) % 60)
        w2, h2 = 150, 210
        od.rounded_rectangle((x, y, x + w2, y + h2), 12, fill=(30 + i * 6, 36 + i * 4, 46 + i * 4),
                             outline=(90, 100, 115), width=2)
    overlay = overlay.filter(ImageFilter.GaussianBlur(2))
    img = Image.blend(img, overlay, 0.25)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((80, 90, 560, 350), 18, fill=(20, 26, 34), outline=GREEN_L, width=3)
    d.text((110, 130), "Leitor de mesa", font=font(46, True), fill=WHITE)
    d.text((110, 190), "TCG", font=font(46, True), fill=GREEN_L)
    d.text((110, 260), "e jogador autónomo", font=font(30), fill=GREY)
    d.text((80, 580), "Guia de utilização — passo a passo", font=font(26, True), fill=WHITE)
    d.text((80, 624), "Magic: The Gathering · Yu-Gi-Oh! (Padrão e Speed Duel)", font=font(20), fill=GREY)
    return img

def end_slide():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.text((W // 2 - 130, 240), "Bom duelo!", font=font(56, True), fill=GREEN_L)
    d.text((W // 2 - 320, 340), "github.com/Yuya-gi-oh/Leitor-de-mesa-tcg-e-jogador-autonomo", font=font(20), fill=GREY)
    d.text((W // 2 - 260, 400), "O código deste vídeo está na pasta demo/ do repositório", font=font(18), fill=GREY)
    return img

def slide_project():
    img = Image.new("RGB", (W, H), BG)
    slide = img
    left_block(slide, "Passo 1", "Abre o projeto no Android Studio",
               ["File → Open, e escolhe a pasta do projeto",
                "Deixa o Gradle sincronizar (versão 8.9)",
                "Corre num dispositivo ou emulador com API 26+",
                "Ou via terminal: ./gradlew assembleDebug"])
    phone(slide, lambda d, w, h: studio_mock(d, w, h))
    return slide

def studio_mock(d, w, h):
    d.rectangle((0, 0, w, 34), fill=(30, 36, 44))
    for i, c in enumerate([(255, 95, 86), (255, 189, 46), (39, 201, 63)]):
        d.ellipse((14 + i * 22, 12, 26 + i * 22, 24), fill=c)
    d.text((80, 8), "LeitorDeMesaTCG", font=font(14), fill=GREY)
    d.rectangle((0, 34, w, h), fill=(16, 20, 26))
    d.text((20, 60), "Sync Gradle… 8.9", font=font(15, True), fill=WHITE)
    for i, msg in enumerate(["Download do Kotlin 2.0.21", "Download do Hilt 2.52", "Índice construído ✔",
                             "Pronto a executar ▶"]):
        ok = i >= 2
        d.text((20, 110 + i * 46), ("✓ " if ok else "› ") + msg, font=font(14),
               fill=GREEN_L if ok else GREY)
    d.rounded_rectangle((20, h - 90, w - 20, h - 60), 6, fill=(26, 32, 40))
    d.rounded_rectangle((20, h - 90, 20 + int((w - 40) * 0.8), h - 60), 6, fill=GREEN)
    d.rounded_rectangle((20, h - 44, w - 20, h - 12), 8, fill=GREEN)
    d.text((w // 2 - 84, h - 40), "Run 'app'  ▶", font=font(15, True), fill=WHITE)

SLIDES = []
SLIDES.append(("Título", title_slide()))
def s_project():
    SLIDES.append(("Projeto", slide_project()))

s_project()

def mk(step, title, bullets, painter):
    img = Image.new("RGB", (W, H), BG)
    left_block(img, step, title, bullets)
    phone(img, painter)
    return img

SLIDES.append(("Início", mk("Passo 2", "Escolhe o jogo",
    ["Abre a app: aparece o ecrã inicial",
     "Toca em Magic: The Gathering ou Yu-Gi-Oh!",
     "Os módulos de regras são aditáveis (Pokémon, etc.)"],
    lambda d, w, h: mock_home(d, w, h, hi_game=1))))

SLIDES.append(("Formato", mk("Passo 3", "Escolhe o formato (Yu-Gi-Oh!)",
    ["Padrão: 40+ cartas, mão inicial de 5",
     "Speed Duel: 20 cartas, mão inicial de 4, 8000 PV",
     "O seletor de formato só aparece no Yu-Gi-Oh!"],
    lambda d, w, h: mock_home(d, w, h, hi_game=1, show_format=True, hi_format=1))))

SLIDES.append(("Dificuldade", mk("Passo 4", "Escolhe a dificuldade da IA",
    ["Fácil — jogadas legais aleatórias",
     "Médio — avaliação heurística do estado",
     "Difícil — minimax com poda alfa-beta",
     "Mestre — Monte Carlo Tree Search"],
    lambda d, w, h: mock_home(d, w, h, hi_game=1, show_format=True, hi_format=1, hi_diff=3))))

SLIDES.append(("Baralho", mk("Passo 5", "Quais são as tuas cartas?",
    ["A app pergunta que cartas vais usar no duelo",
     "Ajusta as quantidades com os botões − e +",
     "O contador mostra o mínimo do formato",
     "Sem paciência? 'Usar baralho de exemplo' preenche tudo"],
    lambda d, w, h: mock_setup(d, w, h))))

SLIDES.append(("Partida", mk("Passo 6", "Joga a partida",
    ["Na tua vez, toca numa jogada legal para a executar",
     "A IA responde sempre dentro das regras",
     "O estado fica sincronizado (vida, mana, campo)"],
    lambda d, w, h: mock_game(d, w, h))))

SLIDES.append(("Explicações", mk("Passo 7", "A IA explica cada jogada",
    ["Cada ação é descrita em texto, em português",
     "Ex.: 'IA: Shivan Dragon ataca Hill Giant'",
     "Aprende as regras enquanto jogas"],
    lambda d, w, h: mock_game(d, w, h))))

SLIDES.append(("Câmara", mk("Passo 8", "Escanear a mesa real",
    ["Toca em 'Escanear mesa' para abrir a câmara",
     "A app deteta cartas físicas e lê o nome por OCR",
     "Aproxima-te, melhora a luz, evita reflexos"],
    lambda d, w, h: mock_camera(d, w, h))))

SLIDES.append(("Correção", mk("Passo 9", "Corrige o estado quando quiseres",
    ["Cada carta reconhecida pode ir para o campo, mão ou cemitério",
     "A correção manual vale sempre mais do que o OCR",
     "Estado corrigido alimenta o motor de regras e a IA"],
    lambda d, w, h: mock_end(d, w, h))))

SLIDES.append(("Fim", mk("Passo 10", "Fim da partida",
    ["Vitória: vida do adversário a zero (ou baralho esgotado)",
     "Usa 'Nova partida' para recomeçar",
     "Experimenta dificuldades e formatos diferentes"],
    lambda d, w, h: mock_end(d, w, h))))

SLIDES.append(("Encerramento", end_slide()))

def render():
    frames = []
    imgs = [s for _, s in SLIDES]
    per = int(SLIDE_SECONDS * FPS)
    for idx, img in enumerate(imgs):
        for _ in range(per):
            frames.append(img)
    # crossfades
    out = []
    for i in range(len(frames)):
        out.append(frames[i])
    writer_out = []
    pos = 0
    for i in range(len(imgs) - 1):
        start = (i + 1) * per - FADE_FRAMES // 2
        end = start + FADE_FRAMES
        base = out[:start]
        for f in range(start, end):
            t = (f - start + 1) / (FADE_FRAMES + 1)
            blended = Image.blend(imgs[i], imgs[i + 1], t)
            base.append(blended)
        out = base + out[end:]
    import imageio.v2 as imageio
    out_path = "/workspace/github__Yuya-gi-oh__Leitor-de-mesa-tcg-e-jogador-autonomo/demo/como-usar.mp4"
    import os
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    writer = imageio.get_writer(out_path, fps=FPS, codec="libx264", quality=6, pixelformat="yuv420p", macro_block_size=16)
    n = 0
    import numpy as np
    for f in out:
        writer.append_data(np.asarray(f))
        n += 1
    writer.close()
    print("frames:", n, "→", out_path)

render()
