/*
 * Copyright (c) 2002-@year@, University of Maryland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the University of Maryland nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Piccolo was written at the Human-Computer Interaction Laboratory www.cs.umd.edu/hcil by Jesse Grosjean
 * under the supervision of Ben Bederson. The Piccolo website is www.cs.umd.edu/hcil/piccolo.
 */
package edu.umd.cs.piccolox.activities;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import edu.umd.cs.piccolo.activities.PInterpolatingActivity;

/**
 * <b>PPositionPathActivity</b> animates through a sequence of points.
 * <p>
 * @version  1.0
 * @author Jesse Grosjean
 */
public class PPositionPathActivity extends PPathActivity {

	protected Point2D[] positions;
	protected Target target;

	public interface Target {
		public void setPosition(double x, double y);
	}

	public PPositionPathActivity(long duration, long stepRate, Target aTarget) {
		this(duration, stepRate, aTarget, null, null);
	}

	public PPositionPathActivity(long duration, long stepRate, Target aTarget, float[] knots, Point2D[] positions) {
		this(duration, stepRate, 1, PInterpolatingActivity.SOURCE_TO_DESTINATION, aTarget, knots, positions);
	}

	public PPositionPathActivity(long duration, long stepRate, int loopCount, int mode, Target aTarget, float[] knots, Point2D[] positions) {
		super(duration, stepRate, loopCount, mode, knots);
		target = aTarget;
		this.positions = positions;
	}

	protected boolean isAnimation() {
		return true;
	}

	public Point2D[] getPositions() {
		return positions;
	}

	public Point2D getPosition(int index) {
		return positions[index];
	}

	public void setPositions(Point2D[] positions) {
		this.positions = positions;
	}	

	public void setPosition(int index, Point2D position) {
		positions[index] = position;
	}

	public void setPositions(GeneralPath path) {
		PathIterator pi = path.getPathIterator(null, 1);
		ArrayList points = new ArrayList();
		float point[]  = new float[6];
		float distanceSum = 0;
		float lastMoveToX = 0;
		float lastMoveToY = 0;
		
		while (!pi.isDone()) {
			int type = pi.currentSegment(point);
			
			switch (type) {
				case PathIterator.SEG_MOVETO:
					points.add(new Point2D.Float(point[0], point[1]));
					lastMoveToX = point[0];
					lastMoveToY = point[1];
					break;

				case PathIterator.SEG_LINETO:
					points.add(new Point2D.Float(point[0], point[1]));
					break;

				case PathIterator.SEG_CLOSE:
					points.add(new Point2D.Float(lastMoveToX, lastMoveToY));
					break;
					
				case PathIterator.SEG_QUADTO:
				case PathIterator.SEG_CUBICTO:
					throw new RuntimeException();
			}
			
			if (points.size() > 1) {
				Point2D last = (Point2D) points.get(points.size() - 2);
				Point2D current = (Point2D) points.get(points.size() - 1);
				distanceSum += last.distance(current);
			}
						
			pi.next();
		}
		
		int size = points.size();
		Point2D newPositions[] = new Point2D[size];
		float newKnots[] = new float[size];
		
		for (int i = 0; i < size; i++) {
			newPositions[i] = (Point2D) points.get(i);
			if (i > 0) {
				float dist = (float) positions[i - 1].distance(newPositions[i]);
				newKnots[i] = newKnots[i - 1] + (dist / distanceSum);
			}
		}
		
		setPositions(newPositions);
		setKnots(newKnots);
	}
	
	public void setRelativeTargetValue(float zeroToOne, int startKnot, int endKnot) {
		Point2D start = getPosition(startKnot);
		Point2D end = getPosition(endKnot);
		target.setPosition(start.getX() + (zeroToOne * (end.getX() - start.getX())),
						   start.getY() + (zeroToOne * (end.getY() - start.getY())));
	}
}
