/** -------------以下地图服务api配置信息自动生成-------------  **/
var api_config = {"traffic_source":{"raster":{"tiles":["http://121.36.99.212:8883/img?t={z}-{x}-{y}"],"tileSize":"256","type":"raster"},"vector":{"tiles":["http://121.36.99.212:18883/amptraffic?t={z}-{x}-{y}"],"type":"vector"}},"url":"http://121.36.99.212:35001"};
/** -------------以上地图服务api配置信息自动生成------------- **/

const DragCircleMode = { ...mapabcgl.Draw.modes.draw_polygon };
window.DragCircleMode = DragCircleMode

const doubleClickZoomDisable = function(ctx) {
    setTimeout(() => {
        if (!ctx.map || !ctx.map.doubleClickZoom) return;
        // Always disable here, as it's necessary in some cases.
        ctx.map.doubleClickZoom.disable();
    }, 0);
}
const dragPanEnable = function(ctx) {
    setTimeout(() => {
        // First check we've got a map and some context.
        if (!ctx.map || !ctx.map.dragPan || !ctx._ctx || !ctx._ctx.store || !ctx._ctx.store.getInitialConfigValue) return;
        // Now check initial state wasn't false (we leave it disabled if so)
        if (!ctx._ctx.store.getInitialConfigValue('dragPan')) return;
        ctx.map.dragPan.enable();
    }, 0);
}
const dragPanDisable = function(ctx) {
    setTimeout(() => {
        if (!ctx.map || !ctx.map.doubleClickZoom) return;
        // Always disable here, as it's necessary in some cases.
        ctx.map.dragPan.disable();
    }, 0);
}

DragCircleMode.onSetup = function(opts) {
    const polygon = this.newFeature({
        type: 'Feature',
        properties: {
            isCircle: true,
            center: []
        },
        geometry: {
            type: 'Polygon',
            coordinates: [
                []
            ]
        }
    });
    this.addFeature(polygon);

    this.clearSelectedFeatures();
    doubleClickZoomDisable(this);
    dragPanDisable(this);
    this.updateUIClasses({ mouse: 'add' });
    this.activateUIButton('polygon');
    this.setActionableState({
        trash: true
    });

    return {
        polygon,
        currentVertexPosition: 0
    };
};

DragCircleMode.onMouseDown = DragCircleMode.onTouchStart = function(state, e) {
    const currentCenter = state.polygon.properties.center;
    if (currentCenter.length === 0) {
        state.polygon.properties.center = [e.lngLat.lng, e.lngLat.lat];
    }
};

DragCircleMode.onDrag = DragCircleMode.onMouseMove = function(state, e) {
    const center = state.polygon.properties.center;
    if (center.length > 0) {
        const distanceInKm = turf.distance(
            turf.helpers.point(center),
            turf.helpers.point([e.lngLat.lng, e.lngLat.lat]), { units: 'kilometers' });
        const circleFeature = turf.circle(center, distanceInKm);
        state.polygon.incomingCoords(circleFeature.geometry.coordinates);
        state.polygon.properties.radiusInKm = distanceInKm;
    }
};

DragCircleMode.onMouseUp = DragCircleMode.onTouchEnd = function(state, e) {
    dragPanEnable(this);
    return this.changeMode('simple_select', { featureIds: [state.polygon.id] });
};

DragCircleMode.onClick = DragCircleMode.onTap = function(state, e) {
    // don't draw the circle if its a tap or click event
    state.polygon.properties.center = [];
};

DragCircleMode.toDisplayFeatures = function(state, geojson, display) {
    const isActivePolygon = geojson.properties.id === state.polygon.id;
    geojson.properties.active = (isActivePolygon) ? 'true' : 'false';
    return display(geojson);
};


const CircleMode = {...mapabcgl.Draw.modes.draw_polygon};
const DEFAULT_RADIUS_IN_KM = 2;

CircleMode.onSetup = function(opts) {
  const polygon = this.newFeature({
    type: "Feature",
    properties: {
      isCircle: true,
      center: []
    },
    geometry: {
      type: 'Polygon',
      coordinates: [[]]
    }
  });

  this.addFeature(polygon);

  this.clearSelectedFeatures();
  doubleClickZoomDisable(this);
  this.updateUIClasses({ mouse: 'add' });
  this.activateUIButton('polygon');
  this.setActionableState({
    trash: true
  });

  return {
    initialRadiusInKm: opts.initialRadiusInKm || DEFAULT_RADIUS_IN_KM,
    polygon,
    currentVertexPosition: 0
  };
};

CircleMode.clickAnywhere = function(state, e) {
  if (state.currentVertexPosition === 0) {
    state.currentVertexPosition++;
    const center = [e.lngLat.lng, e.lngLat.lat];
    const circleFeature = turf.circle(center, state.initialRadiusInKm);
    state.polygon.incomingCoords(circleFeature.geometry.coordinates);
    state.polygon.properties.center = center;
    state.polygon.properties.radiusInKm = state.initialRadiusInKm;
  }
  return this.changeMode("simple_select", { featureIds: [state.polygon.id] });
};

const SimpleSelectMode = mapabcgl.Draw.modes.simple_select;

function createVertex(parentId, coordinates, path, selected) {
  return {
    type: 'Feature',
    properties: {
      meta: 'vertex',
      parent: parentId,
      coord_path: path,
      active: (selected) ? 'true' : 'false'
    },
    geometry: {
      type: 'Point',
      coordinates: coordinates
    }
  };
};
function createMidpoint(parent, startVertex, endVertex, map) {
  const startCoord = startVertex.geometry.coordinates;
  const endCoord = endVertex.geometry.coordinates;

  // If a coordinate exceeds the projection, we can't calculate a midpoint,
  // so run away
  if (startCoord[1] > 85 ||
    startCoord[1] < -85 ||
    endCoord[1] > 85 ||
    endCoord[1] < -85) {
    return null;
  }

  const ptA = map.project([ startCoord[0], startCoord[1] ]);
  const ptB = map.project([ endCoord[0], endCoord[1] ]);
  const mid = map.unproject([ (ptA.x + ptB.x) / 2, (ptA.y + ptB.y) / 2 ]);

  return {
    type: 'Feature',
    properties: {
      meta: 'midpoint',
      parent: parent,
      lng: mid.lng,
      lat: mid.lat,
      coord_path: endVertex.properties.coord_path
    },
    geometry: {
      type: 'Point',
      coordinates: [mid.lng, mid.lat]
    }
  };
};
function createSupplementaryPoints(geojson, options = {}, basePath = null) {
  const { type, coordinates } = geojson.geometry;
  const featureId = geojson.properties && geojson.properties.id;

  let supplementaryPoints = [];

  if (type === 'Point') {
    // For points, just create a vertex
    supplementaryPoints.push(createVertex(featureId, coordinates, basePath, isSelectedPath(basePath)));
  } else if (type === 'Polygon') {
    // Cycle through a Polygon's rings and
    // process each line
    coordinates.forEach((line, lineIndex) => {
      processLine(line, (basePath !== null) ? `${basePath}.${lineIndex}` : String(lineIndex));
    });
  } else if (type === 'LineString') {
    processLine(coordinates, basePath);
  } else if (type.indexOf('Multi') === 0) {
    processMultiGeometry();
  }

  function processLine(line, lineBasePath) {
    let firstPointString = '';
    let lastVertex = null;
    line.forEach((point, pointIndex) => {
      const pointPath = (lineBasePath !== undefined && lineBasePath !== null) ? `${lineBasePath}.${pointIndex}` : String(pointIndex);
      const vertex = createVertex(featureId, point, pointPath, isSelectedPath(pointPath));

      // If we're creating midpoints, check if there was a
      // vertex before this one. If so, add a midpoint
      // between that vertex and this one.
      if (options.midpoints && lastVertex) {
        const midpoint = createMidpoint(featureId, lastVertex, vertex, options.map);
        if (midpoint) {
          supplementaryPoints.push(midpoint);
        }
      }
      lastVertex = vertex;

      // A Polygon line's last point is the same as the first point. If we're on the last
      // point, we want to draw a midpoint before it but not another vertex on it
      // (since we already a vertex there, from the first point).
      const stringifiedPoint = JSON.stringify(point);
      if (firstPointString !== stringifiedPoint) {
        supplementaryPoints.push(vertex);
      }
      if (pointIndex === 0) {
        firstPointString = stringifiedPoint;
      }
    });
  }

  function isSelectedPath(path) {
    if (!options.selectedPaths) return false;
    return options.selectedPaths.indexOf(path) !== -1;
  }

  // Split a multi-geometry into constituent
  // geometries, and accumulate the supplementary points
  // for each of those constituents
  function processMultiGeometry() {
    const subType = type.replace('Multi', '');
    coordinates.forEach((subCoordinates, index) => {
      const subFeature = {
        type: 'Feature',
        properties: geojson.properties,
        geometry: {
          type: subType,
          coordinates: subCoordinates
        }
      };
      supplementaryPoints = supplementaryPoints.concat(createSupplementaryPoints(subFeature, options, index));
    });
  }

  return supplementaryPoints;
}
function constrainFeatureMovement(geojsonFeatures, delta) {
  // "inner edge" = a feature's latitude closest to the equator
  let northInnerEdge = -90;
  let southInnerEdge = 90;
  // "outer edge" = a feature's latitude furthest from the equator
  let northOuterEdge = -90;
  let southOuterEdge = 90;

  let westEdge = 270;
  let eastEdge = -270;

  geojsonFeatures.forEach(feature => {
    const bounds = geojsonExtent(feature);
    const featureSouthEdge = bounds[1];
    const featureNorthEdge = bounds[3];
    const featureWestEdge = bounds[0];
    const featureEastEdge = bounds[2];
    if (featureSouthEdge > northInnerEdge) northInnerEdge = featureSouthEdge;
    if (featureNorthEdge < southInnerEdge) southInnerEdge = featureNorthEdge;
    if (featureNorthEdge > northOuterEdge) northOuterEdge = featureNorthEdge;
    if (featureSouthEdge < southOuterEdge) southOuterEdge = featureSouthEdge;
    if (featureWestEdge < westEdge) westEdge = featureWestEdge;
    if (featureEastEdge > eastEdge) eastEdge = featureEastEdge;
  });


  // These changes are not mutually exclusive: we might hit the inner
  // edge but also have hit the outer edge and therefore need
  // another readjustment
  const constrainedDelta = delta;
  if (northInnerEdge + constrainedDelta.lat > 85) {
    constrainedDelta.lat = 85 - northInnerEdge;
  }
  if (northOuterEdge + constrainedDelta.lat > 90) {
    constrainedDelta.lat = 90 - northOuterEdge;
  }
  if (southInnerEdge + constrainedDelta.lat < -85) {
    constrainedDelta.lat = -85 - southInnerEdge;
  }
  if (southOuterEdge + constrainedDelta.lat < -90) {
    constrainedDelta.lat = -90 - southOuterEdge;
  }
  if (westEdge + constrainedDelta.lng <= -270) {
    constrainedDelta.lng += Math.ceil(Math.abs(constrainedDelta.lng) / 360) * 360;
  }
  if (eastEdge + constrainedDelta.lng >= 270) {
    constrainedDelta.lng -= Math.ceil(Math.abs(constrainedDelta.lng) / 360) * 360;
  }

  return constrainedDelta;
};
function moveFeatures(features, delta) {
  const constrainedDelta = constrainFeatureMovement(features.map(feature => feature.toGeoJSON()), delta);
  features.forEach(feature => {
    const currentCoordinates = feature.getCoordinates();

    const moveCoordinate = (coord) => {
      const point = {
        lng: coord[0] + constrainedDelta.lng,
        lat: coord[1] + constrainedDelta.lat
      };
      return [point.lng, point.lat];
    };
    const moveRing = (ring) => ring.map(coord => moveCoordinate(coord));
    const moveMultiPolygon = (multi) => multi.map(ring => moveRing(ring));

    let nextCoordinates;
    if (feature.type === 'Point') {
      nextCoordinates = moveCoordinate(currentCoordinates);
    } else if (feature.type === 'LineString' || feature.type === 'MultiPoint') {
      nextCoordinates = currentCoordinates.map(moveCoordinate);
    } else if (feature.type === 'Polygon' || feature.type === 'MultiLineString') {
      nextCoordinates = currentCoordinates.map(moveRing);
    } else if (feature.type === 'MultiPolygon') {
      nextCoordinates = currentCoordinates.map(moveMultiPolygon);
    }

    feature.incomingCoords(nextCoordinates);
  });
};
function createSupplementaryPointsForCircle(geojson) {
  const { properties, geometry } = geojson;

  if (!properties.user_isCircle) return null;

  const supplementaryPoints = [];
  const vertices = geometry.coordinates[0].slice(0, -1);
  for (let index = 0; index < vertices.length; index += Math.round((vertices.length / 4))) {
    supplementaryPoints.push(createVertex(properties.id, vertices[index], `0.${index}`, false));
  }
  return supplementaryPoints;
}

SimpleSelectMode.dragMove = function(state, e) {
  // Dragging when drag move is enabled
  state.dragMoving = true;
  e.originalEvent.stopPropagation();

  const delta = {
    lng: e.lngLat.lng - state.dragMoveLocation.lng,
    lat: e.lngLat.lat - state.dragMoveLocation.lat
  };

  moveFeatures(this.getSelected(), delta);

  this.getSelected()
    .filter(feature => feature.properties.isCircle)
    .map(circle => circle.properties.center)
    .forEach(center => {
      center[0] += delta.lng;
      center[1] += delta.lat;
    });

  state.dragMoveLocation = e.lngLat;
};

SimpleSelectMode.toDisplayFeatures = function(state, geojson, display) {
    geojson.properties.active = (this.isSelected(geojson.properties.id)) ?
    'true' : 'false';
    display(geojson);
    this.fireActionable();
    if (geojson.properties.active !== 'true' ||
      geojson.geometry.type === 'Point') return;
    const supplementaryPoints = geojson.properties.user_isCircle ?
      createSupplementaryPointsForCircle(geojson) : createSupplementaryPoints(geojson);
    supplementaryPoints.forEach(display);
};

const DirectMode = mapabcgl.Draw.modes.direct_select;

DirectMode.dragFeature = function(state, e, delta) {
  moveFeatures(this.getSelected(), delta);
  this.getSelected()
    .filter(feature => feature.properties.isCircle)
    .map(circle => circle.properties.center)
    .forEach(center => {
      center[0] += delta.lng;
      center[1] += delta.lat;
    });
  state.dragMoveLocation = e.lngLat;
};

DirectMode.dragVertex = function(state, e, delta) {
  if (state.feature.properties.isCircle) {
    const center = state.feature.properties.center;
    const movedVertex = [e.lngLat.lng, e.lngLat.lat];
    const radius = turf.distance(turf.helpers.point(center), turf.helpers.point(movedVertex), {units: 'kilometers'});
    const circleFeature = turf.circle(center, radius);
    state.feature.incomingCoords(circleFeature.geometry.coordinates);
    state.feature.properties.radiusInKm = radius;
  } else {
    const selectedCoords = state.selectedCoordPaths.map(coord_path => state.feature.getCoordinate(coord_path));
    const selectedCoordPoints = selectedCoords.map(coords => ({
      type: 'Feature',
      properties: {},
      geometry: {
        type: 'Point',
        coordinates: coords
      }
    }));

    const constrainedDelta = constrainFeatureMovement(selectedCoordPoints, delta);
    for (let i = 0; i < selectedCoords.length; i++) {
      const coord = selectedCoords[i];
      state.feature.updateCoordinate(state.selectedCoordPaths[i], coord[0] + constrainedDelta.lng, coord[1] + constrainedDelta.lat);
    }
  }
};

DirectMode.toDisplayFeatures = function (state, geojson, push) {
  if (state.featureId === geojson.properties.id) {
    geojson.properties.active = 'true';
    push(geojson);
    const supplementaryPoints = geojson.properties.user_isCircle ? createSupplementaryPointsForCircle(geojson)
      : createSupplementaryPoints(geojson, {
        map: this.map,
        midpoints: true,
        selectedPaths: state.selectedCoordPaths
      });
    supplementaryPoints.forEach(push);
  } else {
    geojson.properties.active = 'false';
    push(geojson);
  }
  this.fireActionable(state);

}
if(mapabcgl && api_config){
mapabcgl.config.API_URL = api_config.url;
mapabcgl.config.TRAFFIC_SOURCE = api_config.traffic_source;
mapabcgl.config.DEBUG = false;
mapabcgl.accessToken='ec85d3648154874552835438ac6a02b2';
}
