/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped3;

import java.io.Serializable;

/**
 *
 * @author WERNECK
 */
public interface IHashValue extends Comparable<IHashValue>, Serializable {

    byte[] getBytes();

}
