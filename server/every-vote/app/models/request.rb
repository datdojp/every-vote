class Request
  include Mongoid::Document

  # data
  field :message, type: String
  field :n_votes, type: String
  field :starts_at, type: Integer # in seconds since 1970
  field :expires_in, type: Integer # in seconds

  # relations
  embedded_in :user, class_name: User.name, inverse_of: :requests
end