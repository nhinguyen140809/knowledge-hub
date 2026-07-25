import { Card, ScrollShadow } from '@heroui/react'
import { AboutInstance } from '../components/AboutInstance'
import { GuideToc, type TocEntry } from '../components/GuideToc'
import { WALKTHROUGHS } from '../content/walkthroughs'

const TOC: TocEntry[] = [
  ...WALKTHROUGHS.map((w) => ({ id: w.id, label: w.title })),
  { id: 'about', label: 'About this instance' },
]

/**
 * Usage guide for the admin app: task walkthroughs of the workflows admins get
 * stuck on, plus an About block describing the running instance. Two panes on
 * large screens (a contents list on the left, the guide scrolling on the right),
 * stacked below lg.
 */
export function HelpPage() {
  return (
    <div className="grid gap-4 lg:h-full lg:min-h-0 lg:grid-cols-[minmax(200px,1fr)_3fr]">
      <GuideToc entries={TOC} />

      <div className="lg:flex lg:min-h-0 lg:flex-col">
        <ScrollShadow className="lg:min-h-0 lg:flex-1" offset={2}>
          <div className="flex flex-col gap-4">
            {WALKTHROUGHS.map((w) => (
              <div key={w.id} id={w.id} className="scroll-mt-2">
                <Card className="px-6">
                  <Card.Header>
                    <Card.Title className="text-accent text-lg font-bold">{w.title}</Card.Title>
                  </Card.Header>
                  <Card.Content className="flex flex-col gap-2">{w.body}</Card.Content>
                </Card>
              </div>
            ))}

            <AboutInstance />
          </div>
        </ScrollShadow>
      </div>
    </div>
  )
}
