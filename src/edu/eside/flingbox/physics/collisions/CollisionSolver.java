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

package edu.eside.flingbox.physics.collisions;

import edu.eside.flingbox.math.Vector2D;
import edu.eside.flingbox.physics.PhysicBody;

public class CollisionSolver {
	/**
	 * Minimal time unit
	 */
	private final static float DIFFERENTIAL_TIME = 10f / 1000f;
	
	/**
	 * Computes collisions efects to body's
	 * 
	 * @param collision collision descriptor
	 * @param bodyA first colliding body
	 * @param bodyB second colliding body
	 */
	public static void solveCollision(final Collision collision, final PhysicBody bodyA, final PhysicBody bodyB) {
		final Vector2D collisionSense = collision.sense;
		final Vector2D collisionPosition = collision.position;
		
		final float restit = (bodyA.getRestitutionCoeficient() + bodyB.getRestitutionCoeficient()) / 2;
		final float mA = bodyA.getBodyMass();
		final float mB = bodyB.getBodyMass();
		
		Vector2D velA = getVelocityIntoCollisionAxis(collision, bodyA);
		Vector2D velB = getVelocityIntoCollisionAxis(collision, bodyB);
		
		if (!bodyA.isFixed()) { 
			final float vFinalA = bodyB.isFixed() 
						? -velA.i * restit
						: ((1 + restit) * mB * velB.i + velA.i * (mA + restit * mB)) / (mA + mB);
						
			final float normalModule = (vFinalA - velA.i) * mA / DIFFERENTIAL_TIME;
			final float frictionModule = computeFrictionForce(bodyA, normalModule, velA.j);
			
			final Vector2D normalForce = new Vector2D(collisionSense).mul(normalModule);
			final Vector2D frictionForce = new Vector2D(collision.normal).mul(frictionModule);
			
			final Vector2D collisionRelativePoint = new Vector2D(bodyA.getPosition()).sub(collisionPosition);
			
			bodyA.applyForce(normalForce.add(frictionForce), collisionRelativePoint, DIFFERENTIAL_TIME);
		}
		
		if (!bodyB.isFixed()) { 
			final float vFinalB = bodyA.isFixed()
						? -velB.i * restit
						: ((1 + restit) * mA * velA.i + velB.i * (mB + restit * mA)) / (mA + mB);
						
			final float normalModule = (vFinalB - velB.i) * mB / DIFFERENTIAL_TIME;
			final float frictionModule = computeFrictionForce(bodyB, normalModule, velB.j);
						
			final Vector2D normalForce = new Vector2D(collisionSense).mul(normalModule);
			final Vector2D frictionForce = new Vector2D(collision.normal).mul(frictionModule);
						
			final Vector2D collisionRelativePoint = new Vector2D(bodyB.getPosition()).sub(collisionPosition);
						
			bodyB.applyForce(normalForce.add(frictionForce), collisionRelativePoint, DIFFERENTIAL_TIME);
		} 
		
	}
	
	
	/**
	 * Computes friction force's module for a given collision normal.
	 * Friction can be static or dynamic, when body's velocity is not enough to exceed
	 * friction, static friction is applied, else dynamic friction is applied 
	 * 
	 * @param body body witch friction will be computed
	 * @param normal normal force generated by the collision
	 * @param bodyVelocity velocity along collision. Velocity should be decompose
	 *  before pass it as a parameter.
	 * @return Friction force module. it has to be applied along to bodyVelocity
	 */
	private static float computeFrictionForce(PhysicBody body, float normal, float bodyVelocity) {
		float staticFrictionForce = body.getStaticFrictionCoeficient() * normal;
		
		final float currentVel = Math.abs(bodyVelocity) ;
		final float staticFrictionVelDiff = Math.abs(staticFrictionForce * DIFFERENTIAL_TIME / body.getBodyMass());

		/* Check if friction makes too much forces */
		if (currentVel < staticFrictionVelDiff) 
			/* Friction force stops body */
			return -Math.signum(bodyVelocity) * bodyVelocity * body.getBodyMass() / DIFFERENTIAL_TIME;
		else
			/* Friction force can't stop body, and it is constant */
			return -Math.signum(bodyVelocity) * body.getDynamicFrictionCoeficient() * normal;
	}
	
	/**
	 * Obtains a Vector with velocity components proyected to collision's sense.
	 * In the x axis it returns velocity against the collision
	 * 
	 * @param collision collision
	 * @param body body to be collided
	 * @return velocity components proyected
	 */
	private static Vector2D getVelocityIntoCollisionAxis(final Collision collision, final PhysicBody body) {
		final Vector2D collisionSense = collision.sense;
		final Vector2D collisionNormal = collision.normal;
		
		/*
		 *  Get vector from Polygon's center to collision point
		 */
		final Vector2D relativeCollisionPoint = new Vector2D(collision.position).sub(body.getPosition());
		final Vector2D velocityByAngularRotation = 
			Vector2D.normalVector(relativeCollisionPoint) // This returns new Vector2D, so don't copy
			.normalize()
			.mul(relativeCollisionPoint.length() 
					* body.getAngularVelocity()); // / (float) (2f * Math.PI));
		
		/*
		 * Get total body's total velocity at collision point 
		 * NOTE: velocityByAngularRotation is not duplicated since it won't be longer used.
		 */
		final Vector2D totalVelocity = velocityByAngularRotation.add(body.getVelocity()); 

		// Check if body moving away collision
		if (totalVelocity.dotProduct(relativeCollisionPoint) <= 0) 
			return new Vector2D(); // Velocity is Zero
		
		// Discompose into components
		float velAgainstCollision = totalVelocity.dotProduct(collisionSense);
		float velAlongCollision = totalVelocity.dotProduct(collisionNormal);
		
		return new Vector2D(velAgainstCollision, velAlongCollision);
	}
	
}
