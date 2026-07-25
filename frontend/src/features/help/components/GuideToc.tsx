import { Button, Card } from '@heroui/react'

export interface TocEntry {
  id: string
  label: string
}

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** The "On this page" list: each entry smooth-scrolls to a section by id. Labels
 *  wrap rather than truncate, so the full section name always stays readable. */
export function GuideToc({ entries }: { entries: TocEntry[] }) {
  return (
    <Card variant="transparent" className="self-start p-6">
      <Card.Header>
        <Card.Title className="text-accent text-lg font-bold">On this page</Card.Title>
      </Card.Header>
      <Card.Content className="flex flex-col gap-1">
        {entries.map((entry) => (
          <Button
            key={entry.id}
            variant="ghost"
            fullWidth
            onPress={() => scrollTo(entry.id)}
            className="h-auto min-h-9 justify-start py-2 text-left leading-snug font-normal whitespace-normal"
          >
            {entry.label}
          </Button>
        ))}
      </Card.Content>
    </Card>
  )
}
