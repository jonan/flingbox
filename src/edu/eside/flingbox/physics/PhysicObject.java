/*
 *  Flingbox - An OpenSource physics sandbox for Google's Android
 *  Copyright (C) 2009  Jon Ander Peñalba & Endika Gutiérrez
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.eside.flingbox.physics;

import edu.eside.flingbox.math.Point;
import edu.eside.flingbox.math.Vector2D;
import edu.eside.flingbox.physics.collisions.Collider;

public abstract class PhysicObject {
	
	public interface OnMovementListener {
		public void onMovement(Point deplazament, float rotation);
	}
	
	/** Object with MAX_MASS should be impossible to move */
	protected final static float MAX_MASS = Float.MAX_VALUE;
	
	protected final float mBodyMass;
	
	protected final Point mPosition;
	protected float mRotation = 0f;
	
	protected final Vector2D mVelocity;
	protected float mAngularVelocity;
	
	protected Collider mCollider;
	
	
	public PhysicObject(final float bodyMass, final Point position) {
		mBodyMass = bodyMass;
		mPosition = position;
		mVelocity = new Vector2D();
	}
	
	public float getBodyMass() {
		return mBodyMass;
	}
	
	public Point getPosition() {
		return mPosition;
	}
	
	/**
	 * @return the Collider
	 */
	public Collider getCollider() {
		return mCollider;
	}


}
