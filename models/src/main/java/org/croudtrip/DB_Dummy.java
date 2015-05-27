/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * This is an example class for a database table. This DB_Dummy model can be used on
 * both server and client side.
 * Created by Vanessa Lange on 18.04.15.
 */
@Entity(name = "db_dummies")
public class DB_Dummy{

    @Id
    @GeneratedValue
    private long id;

    @Column(name="name", length=25, nullable=false, unique=false)
    private String name;

    @OneToOne
    @Column(name="brother_id", nullable=true)
    // IMPORTANT: foreign keys aren't automatically "filled with data" when querying an object, only their ids.
    private DB_Dummy brother;


    //************************* Constructors *****************************//

    DB_Dummy() {
        // All persisted classes must define a no-arg
        // constructor with at least package visibility
    }

    public DB_Dummy(String name, DB_Dummy brother) {
        this.name = name;
        this.brother = brother;
    }


    //**************************** Methods ******************************//

    public String getName() {
        return name;
    }

    public DB_Dummy getBrother() {
        return brother;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString(){

        String br = "null";
        if(brother != null){
            br = "" + brother.getId();
        }
        return "(DB_Dummy {id: " + id + ", name: \"" +  name + "\", brother_id: " + br + "})";
    }
}
