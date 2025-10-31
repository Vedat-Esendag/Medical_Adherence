package com.example.medicaladherence.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Convert QuerySnapshot to Flow
 */
fun <T> com.google.firebase.firestore.Query.asFlow(mapper: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }

        if (snapshot != null) {
            val items = snapshot.documents.mapNotNull { mapper(it) }
            trySend(items)
        }
    }

    awaitClose {
        listener.remove()
    }
}

/**
 * Convert DocumentSnapshot to Flow
 */
fun <T> com.google.firebase.firestore.DocumentReference.asFlow(mapper: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }

        if (snapshot != null) {
            trySend(mapper(snapshot))
        }
    }

    awaitClose {
        listener.remove()
    }
}
