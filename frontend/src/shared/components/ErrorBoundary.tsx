import { Button } from '@heroui/react'
import { AlertTriangle } from 'lucide-react'
import { Component, type ErrorInfo, type ReactNode } from 'react'
import { EmptyState } from './ui/EmptyState'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  error: Error | null
}

/** Catches render errors below it so one broken screen doesn't take the rest
 *  of the app chrome (sidebar, header) down with it. React only supports
 *  error boundaries as class components — there's no hook equivalent.
 *  Remounting with a fresh `key` (e.g. the route path) clears a stale error
 *  after navigating away; "Try again" clears it in place. */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Uncaught render error:', error, info.componentStack)
  }

  private reset = () => this.setState({ error: null })

  render() {
    if (!this.state.error) return this.props.children

    return (
      <EmptyState
        icon={<AlertTriangle size={28} />}
        description={this.state.error.message || 'Something went wrong.'}
      >
        <Button size="sm" variant="primary" onPress={this.reset}>
          Try again
        </Button>
      </EmptyState>
    )
  }
}
