package com.example.contacts


import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contacts.Adapter.ContactAdapter
import com.example.contacts.ContactFragment.Companion.REQUEST_CODE_DETAIL
import com.example.contacts.Util.callPhoneNumber
import com.example.contacts.databinding.FragmentContactBinding
import de.hdodenhof.circleimageview.CircleImageView
class ContactFragment : Fragment() {
    private lateinit var binding: FragmentContactBinding
    private lateinit var contactAdapter: ContactAdapter
    private var isGridMode = false
    private val contactItems: MutableList<Contact> = mutableListOf()
    private val PICK_IMAGE_REQUEST_CODE = 100
    private lateinit var profileImage: CircleImageView
    private var selectedImageUri: Uri? = null

    // swipe x값
    private var swipeDx = 0f
    private var swipePosition = -1
    companion object {
        const val REQUEST_PHONE_CALL = 1
        const val REQUEST_CODE_DETAIL = -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactBinding.inflate(inflater, container, false)

        // 초기에 어댑터를 생성하고 RecyclerView에 설정
        contactAdapter = ContactAdapter(contactItems, isGridMode)
        binding.RVArea.adapter = contactAdapter
        setLayoutManager() // 초기 레이아웃 매니저 설정
        requirePermission()//연락처 받아오기

        // ItemTouchHelper 추가
        val swipeDirection = if (isGridMode) 0 else ItemTouchHelper.RIGHT
        val touchHelperCallback = ItemTouchHelperCallback(
            0,
            if (isGridMode) 0 else ItemTouchHelper.RIGHT,
            { position ->
                callPhoneNumber(requireActivity(), contactItems[position].phoneNumber)
                contactAdapter.notifyItemChanged(position)
            },
            isGridMode
        ).apply {
            onSwipeListener = { dX, viewHolder ->
                swipeDx = dX
                swipePosition = viewHolder.adapterPosition
                binding.RVArea.invalidateItemDecorations()
            }
        }

        val itemTouchHelper = ItemTouchHelper(touchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.RVArea)

        contactAdapter.productClick = object : ContactAdapter.ProductClick {
            override fun onClick(view: View, position: Int) {
                val detailIntent = DetailActivity.newIntentForDetail(
                    context, contactItems[position]
                )
                startActivityForResult(detailIntent, REQUEST_CODE_DETAIL)
            }
        }
        binding.gridBtn.setOnClickListener {
            isGridMode = true
            setLayoutManager()
            setButtonBackground()
        }
        binding.listBtn.setOnClickListener {
            isGridMode = false
            setLayoutManager()
            setButtonBackground()
        }
        binding.RVArea.adapter = contactAdapter

        // 리싸이클러뷰에서 floatingBtn을 클릭할 때 다이얼로그를 표시
        binding.floatingBtn.setOnClickListener {
            showAddContactDialog()
        }

        // contactItems.clear() //주석풀면 연락처 안받아와집니다!
        contactItems.addAll(ContactsManager.contactsList)

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                performSearch(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        return binding.root
    }

    // 다이얼로그를 표시하는 함수
    private fun showAddContactDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.add_contact_dialog, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val nameEdit = dialogView.findViewById<EditText>(R.id.nameEdit)
        val numberEdit = dialogView.findViewById<EditText>(R.id.numberEdit)
        val emailEdit = dialogView.findViewById<EditText>(R.id.eMailEdit)
        val eventEdit = dialogView.findViewById<EditText>(R.id.eventEdit)
        val addPhotoBtn = dialogView.findViewById<ImageButton>(R.id.addPhotoBtn)

        // addPhotoBtn 클릭 이벤트 설정
        addPhotoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }

        profileImage = dialogView.findViewById(R.id.profileImage)

        dialogView.findViewById<Button>(R.id.saveBt)?.setOnClickListener {
            val name = nameEdit.text.toString()
            val phoneNumber = numberEdit.text.toString()
            val email = emailEdit.text.toString()
            val event = eventEdit.text.toString()

            // 필수 정보가 입력되었는지 확인
            if (name.isNotEmpty() && phoneNumber.isNotEmpty() && email.isNotEmpty() && event.isNotEmpty()) {
                // Contact로 사용자 입력 정보 전달
                var isNewBoolean = true
                if(selectedImageUri == null)
                {
                    isNewBoolean = false
                }

                val newContact = Contact(
                    name,
                    phoneNumber,
                    email,
                    selectedImageUri,
                    R.drawable.un_selsected_image,
                    false,
                    isNewBoolean
                )
                selectedImageUri =null
                ContactsManager.contactsList.add(newContact)
                ContactsManager.contactsList.sortBy { it.name }
                contactItems.clear()
                contactItems.addAll(ContactsManager.contactsList)

                // 다이얼로그 닫기
                dialog.dismiss()

            } else {
                // 필수 정보가 입력되지 않은 경우 토스트 메시지 표시
                Toast.makeText(requireContext(), "입력되지 않은 정보가 있습니다", Toast.LENGTH_SHORT).show()
            }
        }
        dialogView.findViewById<Button>(R.id.cancelBt)?.setOnClickListener {
            // 다이얼로그 닫기
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            if (selectedImageUri != null) {
                // 이미지를 "profileImage"에 설정
                profileImage.setImageURI(selectedImageUri)
            }
        }
        else if (requestCode == REQUEST_CODE_DETAIL && resultCode == RESULT_OK) {

            val updatedBookmark = data?.getBooleanExtra("BOOKMARK", false)
            val phonumber = data?.getStringExtra("PHONE")
            if (updatedBookmark != null && phonumber !=null) {
                for (contact in contactItems) {//포문을 이용해서 컨택트 아이템의 아이템의 북마크에 접근해서 새로운값으로 초기화
                    if (contact.phoneNumber == phonumber) {
                        contact.bookmark = updatedBookmark
                    }
                }
                contactAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setLayoutManager() {
        if (isGridMode) {
            val layoutManager = GridLayoutManager(requireContext(), 3)
            binding.RVArea.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(requireContext())
            binding.RVArea.layoutManager = layoutManager
        }
        contactAdapter = ContactAdapter(contactItems, isGridMode) // 어댑터 다시 설정!!!!!!!!!
        binding.RVArea.adapter = contactAdapter // 어댑터를 다시 설정해주는건 버튼을 눌렀을때 어댑터가 그냥 그리드뷰로 바뀌기 때문에 바인딩해주고ㅓ 초기화

        contactAdapter.productClick = object : ContactAdapter.ProductClick {
            override fun onClick(view: View, position: Int) {
                startActivity(
                    DetailActivity.newIntentForDetail(
                        context, contactItems[position]
                    )
                )
            }
        }
        contactAdapter.notifyDataSetChanged()
    }


    private fun performSearch(query: String) {
        val chosungQuery =extractConsonant(query)

        val filteredList = ContactsManager.contactsList.filter { contact ->//컨택트아이템이 아닌 컨택트 매니저읰 컨택트리스트를 필터
            val chosungName =extractConsonant(contact.name) // 초성변환후 변수 저장
            chosungName.contains(chosungQuery,true) //초성이름이 초성쿼리에 포함된것 확인함
        }
        contactAdapter.updateContactList(filteredList)

    }
    private fun extractConsonant(input: String): String {
        val chosungBuilder = StringBuilder()//초성문자열저장하는거코틀린에 있는 클래스

        for (char in input) {
            val unicode = char.toInt()

            if (unicode in 0xAC00..0xD7A3) {//한글범위하는거입니다.
                val index = (unicode - 0xAC00) / 28 / 21
                val chosung = arrayOf(
                    'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
                    'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
                )[index]
                chosungBuilder.append(chosung)
            } else {
                chosungBuilder.append(char)
            }
        }
        return chosungBuilder.toString()
    }


    private fun setButtonBackground() {
        if (isGridMode) {
            binding.gridBtn.setBackgroundResource(R.drawable.clicked_grid)
            binding.listBtn.setBackgroundResource(R.drawable.unclicked_list)
        } else {
            binding.gridBtn.setBackgroundResource(R.drawable.uncilcked_grid)
            binding.listBtn.setBackgroundResource(R.drawable.clicked_list)
        }
    }


    //실제 연락처 가져오기
    private fun requirePermission() { // 허가
        val permissions = arrayOf(android.Manifest.permission.READ_CONTACTS)
        val permissionCheck = ContextCompat.checkSelfPermission((activity as MainActivity), Manifest.permission.READ_CONTACTS)
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions((activity as MainActivity), permissions, 0)
        }
        else if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            getContact()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContact()
            } else {
                // 권한이 거부되었을 때 처리
            }
        }
    }

    private fun getContact() {
        val resolver: ContentResolver = (activity as MainActivity).contentResolver
        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = resolver.query(phoneUri, projection, null, null, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex(projection[1])
                val numberIndex = cursor.getColumnIndex(projection[2])
                //val emailIndex = cursor.getColumnIndex(projection[3])
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                // val email = cursor.getString(emailIndex)

                // 연락처 정보를 어댑터에 추가
//                contactAdapter.addContact(
//                    Contact(
//                        name,
//                        number,
//                        "22",
//                        profileImageUri = null,
//                        photo = R.drawable.unclicked_user,
//                        bookmark = false,
//                        isNew = false
//                    )
//                )
                ContactsManager.contactsList.add(
                    Contact(
                        name,
                        number,
                        "22",
                        profileImageUri = null,
                        photo = R.drawable.unclicked_user,
                        bookmark = false,
                        isNew = false
                    )
                )
            }
            cursor.close()
        }
    }
//    private fun getContact() {
//        val resolver: ContentResolver = (activity as MainActivity).contentResolver
//        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
//        val projection = arrayOf(
//            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
//            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
//            ContactsContract.CommonDataKinds.Phone.NUMBER,
//        )
//        val cursor = resolver.query(phoneUri, projection, null, null, null)
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
//                val contactId = cursor.getColumnIndex(projection[0])
//                val nameIndex = cursor.getColumnIndex(projection[1])
//                val numberIndex = cursor.getColumnIndex(projection[2])
//                val emailIndex = cursor.getColumnIndex(projection[3])
//                val name = cursor.getString(nameIndex)
//                val number = cursor.getString(numberIndex)
//                val email = cursor.getString(emailIndex)
//
//                // 이메일 주소 가져오기
//                val emailProjection = arrayOf(
//                    ContactsContract.CommonDataKinds.Email.DATA
//                )
//                val emailCursor = resolver.query(
//                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
//                    emailProjection,
//                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
//                    arrayOf(contactId.toString()),
//                    null
//                )
//
//                val emailList = mutableListOf<String>()
//
//                if (emailCursor != null) {
//                    val emailDataIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
//
//                    while (emailCursor.moveToNext()) {
//                        val email = emailCursor.getString(emailDataIndex)
//                        emailList.add(email)
//                    }
//                    emailCursor.close()
//                }
//
//
//                // 연락처 정보를 어댑터에 추가
//                contactAdapter.addContact(
//                    Contact(
//                        name,
//                        number,
//                        emailList.joinToString(", "),
//                        profileImageUri = null,
//                        photo = R.drawable.unclicked_user,
//                        bookmark = false,
//                        isNew = false
//                    )
//                )
//            }
//            cursor.close()
//        }
//    }
}