import { FloatingEdge } from './FloatingEdge'
import { VariantNode } from './VariantNode'

/** The string keys `nodeTypes`/`edgeTypes` are registered under, split out
 *  from the components themselves — component files must export only
 *  components for Fast Refresh to work, so the registration objects React
 *  Flow actually consumes live here instead. */
export const VARIANT_NODE_TYPE = 'variant'
export const FLOATING_EDGE_TYPE = 'floating'

export const graphNodeTypes = { [VARIANT_NODE_TYPE]: VariantNode }
export const graphEdgeTypes = { [FLOATING_EDGE_TYPE]: FloatingEdge }
