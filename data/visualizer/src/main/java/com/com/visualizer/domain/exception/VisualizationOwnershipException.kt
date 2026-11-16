package com.com.visualizer.domain.exception

internal class VisualizationOwnershipException :
    IllegalStateException("Visualization ownership changed before preparation completed\"")

