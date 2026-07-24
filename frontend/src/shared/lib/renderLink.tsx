import { type ComponentProps } from 'react'
import { Link } from 'react-router-dom'

/**
 * Bridges HeroUI's `render` prop — typed for the element it would have rendered,
 * usually a <button> — to a router <Link>, which is an <a>. The two element
 * types are structurally incompatible (onClick handlers disagree), so the cast
 * happens here once instead of at every call site.
 */
export function renderLink(to: string) {
  return (props: object) => <Link {...(props as ComponentProps<typeof Link>)} to={to} />
}
