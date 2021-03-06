package iooojik.ru.phoneblocker.ui.home

import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import iooojik.ru.phoneblocker.R
import iooojik.ru.phoneblocker.localData.callLog.CallLogModel
import iooojik.ru.phoneblocker.localData.whiteList.WhiteListModel
import iooojik.ru.phoneblocker.ui.CallLogAdapter
import java.lang.Long
import java.util.*
import kotlin.collections.ArrayList


class Home : Fragment() {
    private lateinit var rootView : View
    private lateinit var callLogs : MutableList<CallLogModel>
    private lateinit var inflater: LayoutInflater
    private lateinit var myContacts : MutableList<WhiteListModel>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)
        return rootView
    }

    override fun onStart() {
        super.onStart()
        Thread {
            initialize()
        }.start()
    }

    private fun initialize(){
        inflater = requireActivity().layoutInflater
        myContacts = getContactList()
        callLogs = getCallLogs()
        callLogs.reverse()

        requireActivity().runOnUiThread {
            val recView = rootView.findViewById<RecyclerView>(R.id.rec_view_call_log)
            recView.layoutManager = LinearLayoutManager(context)
            recView.adapter = CallLogAdapter(requireContext(), inflater, callLogs, requireActivity())
        }

    }


    private fun getCallLogs() : MutableList<CallLogModel>{
        // получение списка вызовов
        val models : MutableList<CallLogModel> = mutableListOf()
        val calllogsBuffer = ArrayList<String>()
        calllogsBuffer.clear()
        val managedCursor: Cursor = requireActivity().managedQuery(
            CallLog.Calls.CONTENT_URI,
            null, null, null, null
        )
        val number: Int = managedCursor.getColumnIndex(CallLog.Calls.NUMBER)
        val type: Int = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
        val date: Int = managedCursor.getColumnIndex(CallLog.Calls.DATE)

        val duration: Int = managedCursor.getColumnIndex(CallLog.Calls.DURATION)
        while (managedCursor.moveToNext()) {
            val phNumber: String = managedCursor.getString(number)
            val callType: String = managedCursor.getString(type)
            val callDate: String = managedCursor.getString(date)
            val callDayTime = Date(Long.valueOf(callDate))
            val callDuration: String = managedCursor.getString(duration)
            var dir: String? = null
            val dircode = callType.toInt()
            when (dircode) {
                CallLog.Calls.OUTGOING_TYPE -> dir = "OUTGOING"
                CallLog.Calls.INCOMING_TYPE -> dir = "INCOMING"
                CallLog.Calls.MISSED_TYPE -> dir = "MISSED"
            }


            calllogsBuffer.add(
                """
                    Phone Number: $phNumber 
                    Call Type: $dir 
                    Call Date: $callDayTime 
                    Call duration in sec : $callDuration
                """
            )
            val model = CallLogModel(null, getString(R.string.unknown_caller),
                phNumber.toString(), false, callDate.toString(), dir.toString())

            for (md in myContacts){
                if (phNumber == md.phoneNumber){
                    model.name = md.name
                    model.isMyContact = true
                    break
                }
            }

            models.add(model)
        }
        managedCursor.close()
        return models
    }

    private fun getContactList() : MutableList<WhiteListModel> {
        val models : MutableList<WhiteListModel> = mutableListOf()
        //получение контактов
        val cr: ContentResolver = requireActivity().contentResolver
        val cur = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )
        if (cur?.count ?: 0 > 0) {
            while (cur != null && cur.moveToNext()) {
                val id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID))

                val name = cur.getString(
                    cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME
                    )
                )
                if (cur.getInt(
                        cur.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER
                        )
                    ) > 0
                ) {
                    val pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    while (pCur!!.moveToNext()) {
                        var phoneNo = pCur.getString(
                            pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                            )
                        )
                        phoneNo = phoneNo.replace(" ", "", false)
                        phoneNo = phoneNo.replace("-", "", false)
                        Log.i("TAG", "Name: $name")
                        Log.i("TAG", "Phone Number: $phoneNo")

                        val model = WhiteListModel(null, name, phoneNo, true)
                        models.add(model)
                    }
                    pCur.close()
                }
            }
        }
        cur?.close()
        return models
    }
}