import { AlertDialog, Button } from '@heroui/react'
import type { ReactNode } from 'react'

type ConfirmDialogStatus = 'default' | 'accent' | 'success' | 'warning' | 'danger'

interface ConfirmDialogBaseProps {
  icon: ReactNode
  heading: string
  /** Body content — plain text or a custom component. */
  message: ReactNode
  /** The confirm action button. Owns its own onPress/isPending/slot="close" so
   *  each caller keeps control of what happens on confirm. */
  confirmButton: ReactNode
  status?: ConfirmDialogStatus
  className?: string
}

type ConfirmDialogProps = ConfirmDialogBaseProps &
  (
    | {
        /** Element that opens the dialog — usually a Button. */
        trigger: ReactNode
        isOpen?: never
        onOpenChange?: never
      }
    | {
        /** For opening from somewhere that isn't a button the dialog can wrap
         *  itself around — a menu action, for example. */
        trigger?: never
        isOpen: boolean
        onOpenChange: (isOpen: boolean) => void
      }
  )

/** Shared shell for destructive/irreversible confirmations (delete, revoke,
 *  ...). Trigger and confirm button are supplied by the caller — this only
 *  owns the dialog scaffolding (icon/heading/body/footer) and the Cancel
 *  button, which is identical across every use so far. Pass either `trigger`
 *  (the common case) or `isOpen`/`onOpenChange` to drive it from elsewhere;
 *  `AlertDialog.Backdrop` works standalone, with no trigger required. */
export function ConfirmDialog(props: ConfirmDialogProps) {
  const {
    icon,
    heading,
    message,
    confirmButton,
    status = 'danger',
    className = 'sm:max-w-105',
  } = props

  const backdrop = (
    <AlertDialog.Backdrop
      {...('isOpen' in props ? { isOpen: props.isOpen, onOpenChange: props.onOpenChange } : {})}
    >
      <AlertDialog.Container>
        <AlertDialog.Dialog className={className}>
          <AlertDialog.CloseTrigger />
          <AlertDialog.Header>
            <AlertDialog.Icon status={status}>{icon}</AlertDialog.Icon>
            <AlertDialog.Heading>{heading}</AlertDialog.Heading>
          </AlertDialog.Header>
          <AlertDialog.Body>{message}</AlertDialog.Body>
          <AlertDialog.Footer>
            <Button slot="close" variant="tertiary">
              Cancel
            </Button>
            {confirmButton}
          </AlertDialog.Footer>
        </AlertDialog.Dialog>
      </AlertDialog.Container>
    </AlertDialog.Backdrop>
  )

  if (props.trigger) {
    return (
      <AlertDialog>
        {props.trigger}
        {backdrop}
      </AlertDialog>
    )
  }

  return backdrop
}
