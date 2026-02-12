import { parseAnsi, type AnsiSpan } from '../lib/ansi'

export default function AnsiText({ text }: { text: string }) {
  const spans = parseAnsi(text)
  if (spans.length === 1 && !spans[0].color && !spans[0].bold) {
    return <>{text}</>
  }
  return (
    <>
      {spans.map((span, i) => (
        <AnsiSpanView key={i} span={span} />
      ))}
    </>
  )
}

function AnsiSpanView({ span }: { span: AnsiSpan }) {
  const style: React.CSSProperties = {}
  if (span.color) style.color = span.color
  if (span.bgColor) style.backgroundColor = span.bgColor
  if (span.bold) style.fontWeight = 700
  if (span.dim) style.opacity = 0.6
  if (span.italic) style.fontStyle = 'italic'
  const decorations: string[] = []
  if (span.underline) decorations.push('underline')
  if (span.strikethrough) decorations.push('line-through')
  if (decorations.length) style.textDecoration = decorations.join(' ')

  if (Object.keys(style).length === 0) return <>{span.text}</>
  return <span style={style}>{span.text}</span>
}
