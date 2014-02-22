class Friend
  include Mongoid::Document

  field :sns_id, type: String
  field :sns_type, type: String

  embedded_in :friend_list, class_name: FriendList.name, inverse_of: :friends
end