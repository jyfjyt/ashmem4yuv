// IAshmem.aidl
package com.yfjin.server;


interface IAshmem {


     int getVideoWidth();

     int getVideoHeight();

     SharedMemory getVideoMemory();

}