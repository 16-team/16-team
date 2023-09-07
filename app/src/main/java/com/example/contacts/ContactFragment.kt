package com.example.contacts


import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contacts.Adapter.ContactAdapter
import com.example.contacts.Notification.NotificationHelper
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
    private var isClicked = false
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
        //requirePermission()//권한 받아


        // 권한 체크
        val readContactsPermission = Manifest.permission.READ_CONTACTS

        // 권한이 이미 허용되었는지 확인
        if (ContextCompat.checkSelfPermission(requireContext(), readContactsPermission) == PackageManager.PERMISSION_GRANTED) {
            // 권한이 이미 허용된 경우 실행할 코드입니다.
            getContact()
        } else {
            // 권한을 요청
            requestPermission()
        }


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
                    context, contactItems[position],position
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
        val addPhotoBtn = dialogView.findViewById<ImageButton>(R.id.addPhotoBtn)

        val eventOff = dialogView.findViewById<Button>(R.id.eventBtn1)
        val event5s = dialogView.findViewById<Button>(R.id.eventBtn2)
        val event1m = dialogView.findViewById<Button>(R.id.eventBtn3)



        eventOff.setOnClickListener {
            isClicked = !isClicked

            eventOff.setBackgroundResource(if (isClicked) R.color.light_main else R.color.beige)
            event5s.setBackgroundResource(R.color.beige)
            event1m.setBackgroundResource(R.color.beige)

            val notificationHelper = NotificationHelper(requireContext())
            if (isClicked) {

                notificationHelper.cancelNotification()
            }

        }


        event5s.setOnClickListener {
            isClicked = !isClicked

            // 버튼 배경색 변경
            eventOff.setBackgroundResource(R.color.beige)
            event5s.setBackgroundResource(if (isClicked) R.color.light_main else R.color.beige)
            event1m.setBackgroundResource(R.color.beige)

            val notificationHelper = NotificationHelper(requireContext())
            if (isClicked) {
                notificationHelper.scheduleNotification(true)
            } else {
                notificationHelper.cancelNotification()
            }
        }

        event1m.setOnClickListener {
            isClicked = !isClicked

            eventOff.setBackgroundResource(R.color.beige)
            event5s.setBackgroundResource(R.color.beige)
            event1m.setBackgroundResource(if (isClicked) R.color.light_main else R.color.beige)

            val notificationHelper = NotificationHelper(requireContext())
            if (isClicked) {
                notificationHelper.scheduleNotification(false)
            } else {
                notificationHelper.cancelNotification()
            }
        }


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



            // 필수 정보가 입력되었는지 확인
            if (name.isNotEmpty() && phoneNumber.isNotEmpty() && email.isNotEmpty()) {
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
                    isNewBoolean,
                    false
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

//            val updatedBookmark = data?.getBooleanExtra("BOOKMARK", false)
//            val phonumber = data?.getStringExtra("PHONE")
            val resultContact = data?.getParcelableExtra<Contact>(DetailActivity.CONTACT_ITEM)// 객체를 받아옴
            val resultPosition = data?.getIntExtra(DetailActivity.CONTACT_POSITION,0)//position을 받아와줌
            if (resultContact?.bookmark != null && resultPosition != null) {
                contactItems[resultPosition].name = resultContact.name
                contactItems[resultPosition].email = resultContact.email
                contactItems[resultPosition].phoneNumber = resultContact.phoneNumber
                contactItems[resultPosition].bookmark = resultContact.bookmark
                contactItems[resultPosition].profileImageUri = resultContact.profileImageUri
                contactItems[resultPosition].isNew = resultContact.isNew
//                for (contact in contactItems) {//포문을 이용해서 컨택트 아이템의 아이템의 북마크에 접근해서 새로운값으로 초기화
//                    if (contact.phoneNumber == resultContact.phoneNumber) {
//                        contact.bookmark = resultContact.bookmark
//                        contact.name = resultContact.name
//                        contact.name = resultContact.name
//                    }
//                }
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
                        context, contactItems[position],position
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

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(context,"권한 허용",Toast.LENGTH_SHORT).show()
            getContact()
            contactAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(context,"권한을 거부",Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermission() {
        val readContactsPermission = Manifest.permission.READ_CONTACTS
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), readContactsPermission)) {
            getContact() //true
        } else { // false
            permissionLauncher.launch(readContactsPermission)
        }
    }
    private fun getContact() {
        Toast.makeText(context,"연락처를 성공적으로 불러왔습니다!",Toast.LENGTH_SHORT).show()
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
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)

                Toast.makeText(context,"add data : ${name}",Toast.LENGTH_SHORT).show() //최종적으로 삭제 예정
                ContactsManager.contactsList.add(
                    Contact(
                        name,
                        number,
                        "empty",
                        profileImageUri = null,
                        photo = R.drawable.unclicked_user,
                        bookmark = false,
                        isNew = false,
                                isCilked =false
                    )
                )
            }
            ContactsManager.contactsList.sortBy { it.name }
            contactItems.addAll(ContactsManager.contactsList)
            cursor.close()
        }
    }

}