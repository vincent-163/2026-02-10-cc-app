// ANSI SGR escape code parser -> React spans

export interface AnsiSpan {
  text: string;
  bold?: boolean;
  dim?: boolean;
  italic?: boolean;
  underline?: boolean;
  strikethrough?: boolean;
  color?: string;
  bgColor?: string;
}

const COLORS_16: Record<number, string> = {
  30: '#4d4d4d', 31: '#ff5555', 32: '#50fa7b', 33: '#f1fa8c',
  34: '#6272a4', 35: '#ff79c6', 36: '#8be9fd', 37: '#e6edf3',
  90: '#6272a4', 91: '#ff6e6e', 92: '#69ff94', 93: '#ffffa5',
  94: '#d6acff', 95: '#ff92df', 96: '#a4ffff', 97: '#ffffff',
};

const BG_COLORS_16: Record<number, string> = {
  40: '#4d4d4d', 41: '#ff5555', 42: '#50fa7b', 43: '#f1fa8c',
  44: '#6272a4', 45: '#ff79c6', 46: '#8be9fd', 47: '#e6edf3',
  100: '#6272a4', 101: '#ff6e6e', 102: '#69ff94', 103: '#ffffa5',
  104: '#d6acff', 105: '#ff92df', 106: '#a4ffff', 107: '#ffffff',
};

// 256-color palette (first 16 match above, 16-231 are 6x6x6 cube, 232-255 are grays)
function color256(n: number): string {
  if (n < 16) {
    const map: string[] = [
      '#4d4d4d','#ff5555','#50fa7b','#f1fa8c','#6272a4','#ff79c6','#8be9fd','#e6edf3',
      '#6272a4','#ff6e6e','#69ff94','#ffffa5','#d6acff','#ff92df','#a4ffff','#ffffff',
    ];
    return map[n];
  }
  if (n < 232) {
    const idx = n - 16;
    const r = Math.floor(idx / 36);
    const g = Math.floor((idx % 36) / 6);
    const b = idx % 6;
    const toHex = (v: number) => (v === 0 ? 0 : 55 + v * 40).toString(16).padStart(2, '0');
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
  }
  const gray = 8 + (n - 232) * 10;
  const hex = gray.toString(16).padStart(2, '0');
  return `#${hex}${hex}${hex}`;
}

const ESC_RE = /\x1b\[([0-9;]*)m/g;

export function parseAnsi(text: string): AnsiSpan[] {
  const spans: AnsiSpan[] = [];
  let bold = false, dim = false, italic = false, underline = false, strikethrough = false;
  let color: string | undefined;
  let bgColor: string | undefined;
  let lastIndex = 0;

  for (const match of text.matchAll(ESC_RE)) {
    if (match.index > lastIndex) {
      spans.push({ text: text.slice(lastIndex, match.index), bold, dim, italic, underline, strikethrough, color, bgColor });
    }
    lastIndex = match.index + match[0].length;

    const codes = match[1].split(';').map(Number);
    for (let i = 0; i < codes.length; i++) {
      const c = codes[i];
      if (c === 0) { bold = dim = italic = underline = strikethrough = false; color = bgColor = undefined; }
      else if (c === 1) bold = true;
      else if (c === 2) dim = true;
      else if (c === 3) italic = true;
      else if (c === 4) underline = true;
      else if (c === 9) strikethrough = true;
      else if (c === 22) { bold = false; dim = false; }
      else if (c === 23) italic = false;
      else if (c === 24) underline = false;
      else if (c === 29) strikethrough = false;
      else if (c === 39) color = undefined;
      else if (c === 49) bgColor = undefined;
      else if (COLORS_16[c]) color = COLORS_16[c];
      else if (BG_COLORS_16[c]) bgColor = BG_COLORS_16[c];
      else if (c === 38 && codes[i + 1] === 5) { color = color256(codes[i + 2] || 0); i += 2; }
      else if (c === 48 && codes[i + 1] === 5) { bgColor = color256(codes[i + 2] || 0); i += 2; }
    }
  }

  if (lastIndex < text.length) {
    spans.push({ text: text.slice(lastIndex), bold, dim, italic, underline, strikethrough, color, bgColor });
  }

  return spans;
}
