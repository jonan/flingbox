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

/**
 * Contact solver solves contacts between two bodies and applies necessaries 
 * forces over each body.
 * This class has all members static due performance improvement
 */
public class ContactSolver {
	
	/** Prevent solver creation */
	private ContactSolver() { } 
	
	/**
	 * Computes contacts effects to body's
	 * 
	 * @param contact contact descriptor
	 * @param bodyA first colliding body
	 * @param bodyB second colliding body
	 */
	public static void solveContact(final Contact contact) {
		PhysicBody collidingBody; // Colliding body is movable
		PhysicBody collidedBody; // Collided body can or cannot be movable
		 /* Find colliding body */
		if (!contact.bodyInContactA.isFixed()) {
			collidingBody = contact.bodyInContactA; // A is colliding
			collidedBody = contact.bodyInContactB;
		} else if (!contact.bodyInContactB.isFixed()) {
			collidingBody = contact.bodyInContactB; // B is colliding
			collidedBody = contact.bodyInContactA;
		} else
			return; // Collision is between fixed bodies so do nothing
		
		final float restit = (collidingBody.getRestitutionCoeficient() + collidedBody.getRestitutionCoeficient()) / 2;
		/* Get velocity and mass of colliding body */
		final Vector2D collidingVel = getVelocityIntoContactAxis(contact, collidingBody);
		final float collidingMass = collidingBody.getBodyMass();
		
		/* Compute final velocity */
		final float finalVel;
		if (collidedBody.isFixed()) // Other body is fixed
			finalVel = -collidingVel.i * restit;
		else { // If collided body can be moved is a little bit more complicated
			float collidedMass = collidedBody.getBodyMass();
			Vector2D collidedVel = getVelocityIntoContactAxis(contact, collidedBody);
			
			finalVel = ((1 + restit) * collidedMass * collidedVel.i 
					+ collidingVel.i * (collidingMass + restit * collidedMass)) 
					/ (collidingMass + collidedMass);
		}
		/* Compute final normal impulse */
		final float normalImpulseMod = (finalVel - collidingVel.i) * collidingMass;
		/* Get resultant impulse as addition of normal and friction */
		final Vector2D normalImpulse = new Vector2D(contact.normal).mul(normalImpulseMod);
		final Vector2D frictionImpulse = computeFrictionImpulse(collidingBody, normalImpulseMod, collidingVel.j, contact.sense);
		final Vector2D collisionImpuse = normalImpulse.add(frictionImpulse);
		
		/* Where impulse is applied */
		Vector2D contactRelativePoint = new Vector2D(collidingBody.getPosition()).sub(contact.position);
		
		collidingBody.applyImpulse(collisionImpuse, contactRelativePoint);
		
		if (!collidedBody.isFixed()) { // Other body also has an impulse
			contactRelativePoint.set(collidedBody.getPosition()).sub(contact.position);
			collidedBody.applyImpulse(collisionImpuse.negate(), contactRelativePoint);
		}
		
		fixBodysPenetration(contact, collidingBody, collidedBody);
	}
	
	/**
	 * Computes friction force's module for a given contact normal.
	 * Friction can be static or dynamic, when body's velocity is not enough to exceed
	 * friction, static friction is applied, else dynamic friction is applied 
	 * 
	 * @param body body witch friction will be computed
	 * @param normal normal force generated by the contact
	 * @param bodyVelocity velocity along contact. Velocity should be decompose
	 *  before pass it as a parameter.
	 * @param frictionDirection normalized vector with direction.
	 * @return Friction force vector. it has to be applied along to bodyVelocity
	 */
	private static Vector2D computeFrictionImpulse(PhysicBody body, float normal, float bodyVelocity, Vector2D frictionDirection) {
		float staticFrictionForce = body.getStaticFrictionCoeficient() * normal;
		
		final float currentVel = Math.abs(bodyVelocity) ;
		final float staticFrictionVelDiff = Math.abs(staticFrictionForce / body.getBodyMass());
		float module;
		/* Check if friction makes too much force */
		if (currentVel < staticFrictionVelDiff) 
			/* Friction force stops body */
			module = -bodyVelocity * body.getBodyMass();
		else
			/* Friction force can't stop body, and it is constant */
			module = -Math.signum(bodyVelocity) * body.getDynamicFrictionCoeficient() * Math.abs(normal);

		return new Vector2D(frictionDirection).mul(module);
	}
	
	/**
	 * Keeps bodies outside for other bodies
	 * 
	 * @param contact contact descriptor
	 * @param bodyA first body in contact
	 * @param bodyB second body in contact
	 */
	private static void fixBodysPenetration(Contact contact, PhysicBody bodyA, PhysicBody bodyB) {
		
	}
	
	/**
	 * Obtains a Vector with velocity components projected to contact's sense.
	 * In the x axis it returns velocity against the contact
	 * 
	 * @param contact contact
	 * @param body body to be collided
	 * @return velocity components projected
	 */
	private static Vector2D getVelocityIntoContactAxis(final Contact contact, final PhysicBody body) {
		final Vector2D contactNormal = contact.normal;
		final Vector2D contactSense = contact.sense;
		
		/* Get vector from Polygon's center to contact point  */
		final Vector2D relativeContactPoint = new Vector2D(contact.position).sub(body.getPosition());
		final Vector2D velocityByAngularRotation = 
			Vector2D.normalVector(relativeContactPoint) // This returns new Vector2D, so don't copy
			.normalize()
			.mul(relativeContactPoint.length() 
					* body.getAngularVelocity());
		
		/* Get total body's total velocity at contact point 
		 * NOTE: velocityByAngularRotation is not duplicated since it won't be longer used.
		 */
		final Vector2D totalVelocity = velocityByAngularRotation.add(body.getImpulse().mul(1f / body.getBodyMass())); 

		/* Check if body moving away contact */
		if (!totalVelocity.isAtSameSide(relativeContactPoint /*contact.getBodysSide(body)*/)) 
			return new Vector2D(); // Velocity is Zero
		
		/* Decompose into components */
		float velAgainstContact = totalVelocity.dotProduct(contactNormal);
		float velAlongContact = totalVelocity.dotProduct(contactSense);
		
		return new Vector2D(velAgainstContact, velAlongContact);
	}
	
}
